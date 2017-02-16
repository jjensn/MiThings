Rails.application.routes.draw do
  devise_for :users, skip: [:registrations, :recoverable], path_names: { sign_in: '/', sign_out: 'logout' }, controllers: { omniauth_callbacks: 'omniauth_callbacks' }
  get 'landing/index'
  root 'landing#index'

  resources :hubs, only: [:index, :create, :destroy]
  resources :contact, only: [:index]
  resources :dashboard, only: [:index, :show]

  api_domain = 'dev-api'
  api_domain = 'api' if Rails.env == 'production'

  constraints subdomain: api_domain do
    scope module: 'api' do
      namespace :v1 do
        match '/' => Api::V1::RgbwController, :anchor => false, :via => [:get]
      end
      namespace :v2 do
        resources :rgbw
      end
    end
  end
  # For details on the DSL available within this file, see http://guides.rubyonrails.org/routing.html
end
