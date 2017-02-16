class AddHeartbeatToHubs < ActiveRecord::Migration[5.0]
  def change
    add_column :hubs, :heartbeat, :timestamp
  end
end
