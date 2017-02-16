class CreateHubs < ActiveRecord::Migration[5.0]
  def change
    create_table :hubs do |t|
      t.belongs_to :user, index: true
      t.string :nick
      t.string :mac

      t.timestamps
    end
  end
end
