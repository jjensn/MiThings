require_relative 'boot'

require 'rails/all'

# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)

module App
  class Application < Rails::Application
    # Settings in config/environments/* take precedence over those specified here.
    # Application configuration should go into files in config/initializers
    # -- all .rb files in that directory are automatically loaded.
    # config.web_console.whitelisted_ips = '172.18.0.1'
    logger           = ActiveSupport::Logger.new(STDOUT)
    logger.formatter = config.log_formatter
    config.log_tags  = [:subdomain, :uuid]
    config.logger    = ActiveSupport::TaggedLogging.new(logger)
    config.cache_store = :redis_store, { 
      host: ENV['REDIS_SERVER'],
      port: 6379,
      db: 2,
      password: ENV['REDIS_PASS'],
      namespace: "#{Rails.env}_cache"
    }
  end
end
