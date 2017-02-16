class AddMaxHubsToUsers < ActiveRecord::Migration[5.0]
  def change
    add_column :users, :max_hubs, :integer
  end
end
