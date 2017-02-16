class ActionLog < ApplicationRecord
  belongs_to :hub
  alias_attribute :processed_last_command, :processed
end
