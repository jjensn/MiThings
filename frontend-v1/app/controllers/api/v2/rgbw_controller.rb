module Api
  module V2
    class RgbwController < ApplicationController
      skip_before_action :verify_authenticity_token
      before_action :authenticate

      def index
        # https://www.codementor.io/ruby-on-rails/tutorial/how-to-configure-your-first-rails-rest-api 
        # when doing authentication 
        skip_policy_scope
        render json: {status: 'SUCCESS', message: 'Loaded all posts'}, status: :ok
      end

      def update
        # stopped here -- need to create the status initially in JSON instead of using symbols
        skip_policy_scope
        update_status = status_params
        @hub = Hub.find_by(mac: update_status[:id])
        if @hub.status.nil?
          @hub.status = {}
          for i in 0..4
            @hub.status["group_#{i}"] = {:power => 'on', :brightness => '100', :color => '0'}
          end
        end
        #@actionlog = new ActionLog.New
        @hub.status["group_#{update_status[:group]}"][update_status[:api_action].to_s] = update_status[:value]
        @hub.action_log.new(action: update_status[:api_action], value: update_status[:value], group: update_status[:group])
        cmd_arr = generate_command(update_status[:api_action], update_status[:value], update_status[:group])
        if !cmd_arr.is_a?(Array)
          # stopped here
        end
        groom_command(cmd_arr)
        render json: cmd_arr
        return
        config.redis.publish("cmdQueue", fullCmd.to_json)
        if @hub.save
          render json: @hub.status
        else
          render json: @hub.errors, status: :unprocessable_entity
        end
      end

      protected

      def status_params
        params.permit(:id, :api_action, :value, :group)
      end

      def authenticate
        authenticate_or_request_with_http_token do |token, _|
          User.find_by(token: token).present?
        end
      end

      private

      def generate_command(action, value, group)
        if(action == "power")
          if(value == "on")
            case group
              when 0
                [ 0x42, 0x00, 0x55 ]
              when 1
                [ 0x45, 0x00, 0x55 ]
              when 2
                [ 0x47, 0x00, 0x55 ]
              when 3
                [ 0x49, 0x00, 0x55 ]
              when 4
                [ 0x4B, 0x00, 0x55 ]
              else
                false
            end
          elsif(value == "off")
            case group
              when 0
                [ 0x41, 0x00, 0x55 ]
              when 1
                [ 0x46, 0x00, 0x55 ]
              when 2
                [ 0x48, 0x00, 0x55 ]
              when 3
                [ 0x4A, 0x00, 0x55 ]
              when 4
                [ 0x4C, 0x00, 0x55 ]
              else
                false
            end
          end
        elsif(action == "brightness")
          onCmd = generateCommand("power", "on", group)
          [ onCmd, [ 0x4E, (value.to_i/4) + 2, 0x55 ] ]
        elsif(action == "color")
          if(value == 0)
            # white
            case group
              when 0
                [ 0xC2, 0x00, 0x55 ]
              when 1
                [ 0xC5, 0x00, 0x55 ]
              when 2
                [ 0xC7, 0x00, 0x55 ]
              when 3
                [ 0xC9, 0x00, 0x55 ]
              when 4
                [ 0xCB, 0x00, 0x55 ]
              else
                false
            end
          else
            onCmd = generateCommand("power", "on", group)
            [ onCmd, [ 0x40, value, 0x55 ]]
          end
        end
      end

      def groom_command(cmd_array)
        if cmd_array.flatten.count > 3
          result = []
          cmd_array.each { |x| result << Base64.encode64(x.pack('c*')) }
          return result
        elsif cmd_array.flatten.count == 3
          return [Base64.encode64(cmdArray.pack('c*'))]
        end
      end
    end
  end
end
