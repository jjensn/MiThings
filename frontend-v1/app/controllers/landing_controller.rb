class LandingController < ApplicationController
  protect_from_forgery with: :exception

  def index
    skip_policy_scope
    
    if user_signed_in?
      redirect_to :controller => 'dashboard', :action => 'index'
      return
    end

    @conn_count = Array.new
    #influxdb = InfluxDB::Rails.client epoch: 's'
    #InfluxDB::Rails.client.query 'SELECT MEAN(count),time FROM conn_count group by time(1d) WHERE time > now() - 1w limit 1000;' do |name, tags, points|

    # InfluxDB::Rails.client.query 'select MODE(count) from conn_count WHERE time > now() - 6d group by time(2d)' do |name, tags, points|
    #   logger.debug("#{points.inspect}")
    #   points.each do |pt|
    #     @conn_count << pt.map{|k,v| (k=="time" ? Time.parse(v).to_time.to_i : v) }
    #   end
    #   # Time.parse(t[:time]
    #   logger.debug "#{@conn_count.inspect}"
    # end

    @tai = { title: 'Hello!', icon: 'fa-thumbs-o-up' }
  end
end
