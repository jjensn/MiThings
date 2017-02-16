class SetMaxHubsDefaultValue < ActiveRecord::Migration[5.0]
  def up
    change_column :users, :max_hubs, :integer, default: 5
    change_column :users, :admin, :boolean, default: false
  end

  def down
    change_column :users, :max_hubs, :integer, default: nil
    change_column :users, :admin, :boolean, default: nil
  end
end
