import { Link, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  LayoutDashboard,
  ShoppingBag,
  ChefHat,
  Calendar,
  ListChecks,
  User,
} from 'lucide-react'

const menuItems = [
  { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/pantry', label: 'Pantry', icon: ShoppingBag },
  { path: '/recipes', label: 'Recipes', icon: ChefHat },
  { path: '/meal-plan', label: 'Meal Plan', icon: Calendar },
  { path: '/shopping-list', label: 'Shopping List', icon: ListChecks },
  { path: '/profile', label: 'Profile', icon: User },
]

const Sidebar = () => {
  const location = useLocation()

  return (
    <aside className="hidden md:block w-64 min-h-[calc(100vh-4rem)] border-r border-slate-200/80 bg-white/50">
      <nav className="p-4 space-y-1 sticky top-20">
        {menuItems.map((item, i) => {
          const Icon = item.icon
          const isActive = location.pathname === item.path

          return (
            <Link key={item.path} to={item.path}>
              <motion.div
                initial={false}
                className={`relative flex items-center gap-3 px-4 py-3 rounded-xl transition-colors ${
                  isActive ? 'bg-primary-50 text-primary-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                }`}
                whileHover={{ x: 4 }}
                whileTap={{ scale: 0.98 }}
              >
                {isActive && (
                  <motion.div
                    layoutId="sidebar-active"
                    className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-primary-500 rounded-r-full"
                    transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                  />
                )}
                <Icon size={20} className={isActive ? 'text-primary-600' : ''} />
                <span className="font-medium">{item.label}</span>
              </motion.div>
            </Link>
          )
        })}
      </nav>
    </aside>
  )
}

export default Sidebar
