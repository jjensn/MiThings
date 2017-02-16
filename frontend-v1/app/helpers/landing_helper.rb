module LandingHelper
  def signin_path(provider)
    "users/auth/#{provider.to_s}"
  end
end
