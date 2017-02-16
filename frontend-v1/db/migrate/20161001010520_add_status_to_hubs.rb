class AddStatusToHubs < ActiveRecord::Migration[5.0]
  def change
    add_column :hubs, :status, :json
  end
end
