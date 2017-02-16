class DashboardPolicy < Struct.new(:user, :dashboard)
  def show?
    true
  end
end
