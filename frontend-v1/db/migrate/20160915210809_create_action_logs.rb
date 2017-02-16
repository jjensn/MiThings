class CreateActionLogs < ActiveRecord::Migration[5.0]
  def change
    create_table :action_logs do |t|
      t.belongs_to :hub, index: true
      t.timestamp :timestamp
      t.string :action
      t.string :value
      t.integer :group

      t.timestamps
    end
  end
end
