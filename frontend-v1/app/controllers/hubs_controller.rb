class HubsController < ApplicationController
  protect_from_forgery with: :exception
  before_action :authenticate_user!
  before_action :set_view_requirements

  def index
    @hub = Hub.new
    @hubs = policy_scope(Hub)
  end

  def create
    @user = current_user
    @hub = Hub.new
    @hub = @user.hubs.new(hub_params)
    authorize @hub
    if @user.save
      flash[:success] = "#{@hub.nick} added."
      redirect_to action: "index"
    else
      skip_authorization
      render :index
    end    
  end

  def destroy
    set_hub
    if @hub.present?
      authorize @hub
      if @hub.destroy
        flash[:success] = "Hub #{@hub[:mac]} successfully deleted."
        redirect_to action: "index"
      else
        skip_authorization
        flash[:error] = "Could not delete #{hub[:mac]}, please retry."
        render :index
      end      
    end  
  end

  private

  def hub_params
    params.require(:hub).permit(policy(@hub).permitted_attributes)                                    
  end

  def set_hub
    @hub = Hub.find_by(id: params[:id])
  end

  def set_view_requirements
    @tai = { :title => "Hub Management", :icon => 'fa-lightbulb-o'}
    @hubs = policy_scope(Hub)
  end

end
