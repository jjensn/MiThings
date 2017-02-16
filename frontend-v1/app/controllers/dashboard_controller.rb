class DashboardController < ApplicationController
  protect_from_forgery with: :exception
  before_action :authenticate_user!
  
  def index
    skip_policy_scope
    @tai = { title: 'Dashboard', icon: 'fa-tachometer' }
  end

  def show
    skip_policy_scope
    if params[:id].eql?('api')
      render json: params[:id]
    else
      render layout: "dashboard/api"
    end
  end

  def preview
    skip_policy_scope
    render json: "hello"
  end
end
