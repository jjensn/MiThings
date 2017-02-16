class AddProcessedToActionLogs < ActiveRecord::Migration[5.0]
  def change
    add_column :action_logs, :processed, :bool, default: false
  end
end
