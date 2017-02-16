class RemoveTimestampFromActionLogs < ActiveRecord::Migration[5.0]
  def change
    remove_column :action_logs, :timestamp, :timestamp
  end
end
