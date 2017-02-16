class ContactController < ApplicationController
  def index
    @tai = { :title => "Contact", :icon => 'fa-envelope-o'}
  end
end
