class ApplicationController < ActionController::Base
  include Pundit
  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception

  # Globally rescue Authorization Errors in controller.
  # Returning 403 Forbidden if permission is denied
  rescue_from Pundit::NotAuthorizedError, with: :permission_denied

  # Enforces access right checks for individuals resources
  #after_filter :verify_authorized, :except => :index, unless: :devise_controller?

  # Enforces access right checks for collections
  #after_filter :verify_policy_scoped, :only => :index

  def after_sign_in_path_for(resource)
    dashboard_index_url
  end

  private
  def permission_denied
    head 403
  end

  protected
  def authenticate_user!
    if user_signed_in?
      super
    else
      redirect_to root_url
      ## if you want render 404 page
      ## render :file => File.join(Rails.root, 'public/404'), :formats => [:html], :status => 404, :layout => false
    end
  end

end
