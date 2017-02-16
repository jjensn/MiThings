class AddAdjustedMacToHubs < ActiveRecord::Migration[5.0]
  def change
    add_column :hubs, :adjusted_mac, :string
  end
end
