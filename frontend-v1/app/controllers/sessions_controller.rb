class SessionsController < ApplicationController
  def create

    auth = request.env['omniauth.auth']
    #
    # Find an identity here
    @identity = Identity.find_with_omniauth(auth)

    if @identity.nil?
      # If no identity was found, create a brand new one here
      @identity = Identity.create_with_omniauth(auth)
    end

    if signed_in?
      if @identity.user == current_user
        # User is signed in so they are trying to link an identity with their
        # account. But we found the identity and the user associated with it 
        # is the current user. So the identity is already associated with 
        # this user. So let's display an error message.
        redirect_to dashboard_index_url, notice: "Already linked that account!"
      else
        # The identity is not associated with the current_user so lets 
        # associate the identity
        @identity.user = current_user
        @identity.save
        redirect_to dashboard_index_url, notice: "Successfully linked that account!"
      end
    else
      if @identity.user.present?
        # The identity we found had a user associated with it so let's 
        # just log them in here
        # self.current_user = @identity.user
        #redirect_to root_url, notice: "Signed in!"
      else
        # No user associated with the identity so we need to create a new one
        #redirect_to new_user_url, notice: "Please finish registering"
        @auth = Identity.create_with_omniauth(auth)
      end
      self.current_user = @identity.user
      #session[:user_id] = user.id
      redirect_to dashboard_index_url, notice: "Signed in!"
      #render text: request.env['omniauth.auth'].to_yaml
    end
  end
  
  def destroy
    self.current_user = nil
    redirect_to root_url, notice: "Signed out!"
  end
end
