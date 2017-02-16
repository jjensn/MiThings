require 'sinatra/base'

module Api::V1
  class RgbwController < Sinatra::Base
    # app.rb
    require 'base64'

    Bundler.require(:default)

    LOG_FILE = 'mithings-frontend.log'.freeze
    REDIS_DB = { 'development' => 0, 'test' => 0, 'production' => 1 }.freeze

    configure do
      set :logger, Logging.logger['WebAPI']
      set :redis, Redis.new(host: (ENV['REDIS_SERVER']).to_s,
                            password: (ENV['REDIS_PASS']).to_s,
                            db: (REDIS_DB[ENV['RAILS_ENV']]))

      settings.logger.level = :debug
      settings.logger.add_appenders( \
        Logging.appenders.stdout
      )
    end

    helpers do
      def authenticate(mac)
        if env['HTTP_AUTHORIZATION'] && env['HTTP_AUTHORIZATION'].split(' ').length == 2
          token = env['HTTP_AUTHORIZATION'].split(' ')[1]
        else
          resp = { result: { status: 'error', message: 'Invalid token' } }
          settings.logger.info "#{JSON.pretty_generate(resp)}"
          halt 406, JSON.pretty_generate(resp)
        end
        @user = User.find_by(token: token)
        if @user.present?
          @hub = @user.hubs.find_by(mac: mac)
          return @hub if @hub.present?
          resp = { result: { status: 'error', message: 'Hub not found' } }
          settings.logger.info "#{JSON.pretty_generate(resp)}"
          halt 406, JSON.pretty_generate(resp)
        end
        resp = { result: { status: 'error', message: 'Authentication failed' } }
        settings.logger.info "#{JSON.pretty_generate(resp)}"
        halt 401, JSON.pretty_generate(resp)
      end

      def mac!
        unless env['HTTP_MAC'] && /([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})/ =~ env['HTTP_MAC']
          resp = { result: { status: 'error', message: 'Invalid MAC address' } }
          settings.logger.info "#{JSON.pretty_generate(resp)}"
          halt 406, JSON.pretty_generate(resp)
        end
        return env['HTTP_MAC'].to_s.upcase
      end
    end

    get '/?:model?/?' do
      if REDIS_DB[ENV['RAILS_ENV']].to_i.zero?
        '# dev'
      elsif REDIS_DB[ENV['ENV_NAME']] == 1
        'Interface coming soon.'
      else
        'eek!'
      end
    end

    get '/rgbw/status/?:group?/?' do
      unless params[:group].nil?
        raise Sinatra::NotFound unless params[:group] =~ /^[0-4+]*$/
      end

      mac = mac!
      @hub = authenticate(mac)

      return JSON.pretty_generate(get_hub_status(@hub)['status']["group_#{group}"])
    end

    get '/rgbw/:action/:value/?:group?/?' do
      raise Sinatra::NotFound unless %w(power brightness color).include? params[:action]

      unless params[:group].nil?
        raise Sinatra::NotFound unless params[:group] =~ /^[0-4+]*$/
      end

      mac = mac!

      @hub = authenticate(mac)

      action = params[:action]
      value = params[:value]
      group = params[:group].to_i

      settings.logger.info "MAC: #{mac}, ACTION: #{action}, GROUP: #{group}, PATH: #{request.fullpath}"

      # need to save this for statuses
      original_value = value

      if action == 'color'
        # need to do some additional work around color commands
        value = "##{value}" unless value[0].eql?('#')

        if %w(#FFFFFF #FDF8EC #EFF9FF #F7F8F3 #F3F5F8).include? value
          # in ST, this is white, so we need to set the bulbs white
          value = 0
        else
          begin
            color = Color::RGB.from_html(value)
          rescue => e
            settings.logger.error e.to_s
            resp = { result: { status: 'error', message: 'Not a valid HTML color code' } }
            halt 406, JSON.pretty_generate(resp)
          end
          value = ((-(color.to_hsl.hue - 240) % 360) / 360.0 * 255.0).to_i
          if value == 127
            # smartthings workaround
            value = 0
          end
          settings.logger.info "Color converted to decimal: #{value}"
        end
      end

      cmdArr = groomCmds(generateCommand(action, value, group))

      unless cmdArr
        settings.logger.error "Failed to generate a command for #{request.fullpath}"
        raise Sinatra::NotFound
      end

      if @hub.status.nil?
        @hub.status = {}
        for i in 0..4
          @hub.status["group_#{i}"] = {:power => 'on', :brightness => '100', :color => '0', :hex => '#FFFFFF'}
        end
      end

      if group.zero?
        # state of all groups should be set
        0.upto(4) do |i|
          @hub.status["group_#{i}"][action] = original_value.to_s
          if action == 'color' || action == 'brightness'
            # need to also say that these bulbs are on
            @hub.status["group_#{i}"]['power'] = 'on'
            @hub.status["group_#{i}"]['hex'] = original_value if action == 'color'
          end
        end
      else
        @hub.status["group_#{group}"][action] = original_value.to_s
        if action == 'color' || action == 'brightness'
          @hub.status["group_#{group}"]['power'] = 'on'
          @hub.status["group_#{group}"]['hex'] = original_value if action == 'color'
        end
      end

      if @hub.action_log.new(action: action, value: value, group: group) && @hub.save!
        redis_env = 'development'
        redis_env = 'production' if Rails.env == 'production'
        begin
          settings.redis.publish("#{redis_env}_cmdQueue", { cmds: cmdArr, 
            action_log_id: @hub.action_log.maximum(:id), mac: @hub.adjusted_mac }.to_json)
        rescue
          resp = { result: { status: 'error', message: 'Failed to send command data to hub' } }
          halt 500, JSON.pretty_generate(resp)
        end
      end
      
      @updated_hub = authenticate(mac)
      return JSON.pretty_generate(get_hub_status(@updated_hub))
    end

    get '/admin/title/:title/message/:message/image/:image/url/:url/:pass' do
      if params[:pass] == 'mrpeter1'
        notification = { title: params[:title], message: params[:message], image: params[:image], hasMessage: 1, url: params[:url] }
        settings.redis.set('notification', notification.to_json)
      end
    end

    get '/admin/clearnotification/:pass' do
      if params[:pass] == 'mrpeter1'
        notification = { title: '', message: '', image: '', hasMessage: 0 }
        settings.redis.set('notification', notification.to_json)
      end
    end

    get '/status/healthcheck' do
      # TODO: check redis connect
      'all is well.'
    end

    protected

    def get_hub_status(hub)
      last_action_log = hub.action_log.last
      hub.slice(:nick, :mac, :updated_at, :status).merge(last_action_log.slice(:processed_last_command))
    end

    def generateCommand(action, value, group)
      if action == 'power'
        if value == 'on'
          case group
          when 0
            [0x42, 0x00, 0x55]
          when 1
            [0x45, 0x00, 0x55]
          when 2
            [0x47, 0x00, 0x55]
          when 3
            [0x49, 0x00, 0x55]
          when 4
            [0x4B, 0x00, 0x55]
          else
            false
          end
        elsif value == 'off'
          case group
          when 0
            [0x41, 0x00, 0x55]
          when 1
            [0x46, 0x00, 0x55]
          when 2
            [0x48, 0x00, 0x55]
          when 3
            [0x4A, 0x00, 0x55]
          when 4
            [0x4C, 0x00, 0x55]
          else
            false
          end
        end
      elsif action == 'brightness'
        onCmd = generateCommand('power', 'on', group)
        [onCmd, [0x4E, (value.to_i / 4) + 2, 0x55]]
      elsif action == 'color'
        if value.zero?
          # white
          case group
          when 0
            [0xC2, 0x00, 0x55]
          when 1
            [0xC5, 0x00, 0x55]
          when 2
            [0xC7, 0x00, 0x55]
          when 3
            [0xC9, 0x00, 0x55]
          when 4
            [0xCB, 0x00, 0x55]
          else
            false
          end
        else
          onCmd = generateCommand('power', 'on', group)
          [onCmd, [0x40, value, 0x55]]
        end
      end
    end

    def groomCmds(cmdArray)
      if cmdArray.flatten.count > 3
        result = []
        cmdArray.each { |x| result << Base64.encode64(x.pack('c*')) }
        return result
      elsif cmdArray.flatten.count == 3
        return [Base64.encode64(cmdArray.pack('c*'))]
      end
    end
  end
end
