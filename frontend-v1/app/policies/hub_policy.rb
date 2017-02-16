class HubPolicy < ApplicationPolicy
  class Scope < Scope
    def resolve
      scope
    end
  end

  def destroy?
    record.user == user
  end

  def create?
    true
  end

  def permitted_attributes
    # if user.admin?
    #   [:title, :body, :special]
    # else
      [:nick, :mac]
    #end
  end
end
