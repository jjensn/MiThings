class Hub < ApplicationRecord
  has_many :action_log
  validates :mac, uniqueness: { :message => "The MAC address is already in use." }
  validates :nick, presence: { :message => "Descriptions cannot be blank."}
  validates_format_of :mac, :with => /\A([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})\Z/i, :message => "MAC addresses should be in format aa:bb:cc:dd:ee:ff" 
  belongs_to :user

  before_save :uppercase_mac
  before_save :adjust_mac

  def uppercase_mac
    mac.upcase!
  end

  def adjust_mac
    last2 = mac.split(':').last.to_i(16) + 1
    write_attribute(:adjusted_mac, ((mac.to_s[0..14]) + last2.to_s(16)).upcase)
  end
end
