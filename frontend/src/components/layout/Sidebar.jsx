import { Link, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  ShoppingBag,
  ChefHat,
  Calendar,
  ListChecks,
  User,
} from 'lucide-react'

/**
 * Sidebar Component
 * 
 * Side navigation menu with links to main sections.
 * Highlights active route.
 * 
 * @author MealCraft Team
 */
const Sidebar = () => {
  const location = useLocation()

  const menuItems = [
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/pantry', label: 'Pantry', icon: ShoppingBag },
    { path: '/recipes', label: 'Recipes', icon: ChefHat },
    { path: '/meal-plan', label: 'Meal Plan', icon: Calendar },
    { path: '/shopping-list', label: 'Shopping List', icon: ListChecks },
    { path: '/profile', label: 'Profile', icon: User },
  ]

  return (
    <aside className="w-64 bg-white shadow-lg min-h-[calc(100vh-4rem)]">
      <nav className="p-4">
        <ul className="space-y-2">
          {menuItems.map((item) => {
            const Icon = item.icon
            const isActive = location.pathname === item.path

            return (
              <li key={item.path}>
                <Link
                  to={item.path}
                  className={`flex items-center px-4 py-3 rounded-lg transition-colors ${
                    isActive
                      ? 'bg-primary-100 text-primary-700 font-semibold'
                      : 'text-gray-700 hover:bg-gray-100'
                  }`}
                >
                  <Icon size={20} className="mr-3" />
                  {item.label}
                </Link>
              </li>
            )
          })}
        </ul>
      </nav>
    </aside>
  )
}

export default Sidebar




