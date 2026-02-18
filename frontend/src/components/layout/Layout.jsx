import { Outlet } from 'react-router-dom'
import { Link, useLocation } from 'react-router-dom'
import Navbar from './Navbar'
import Sidebar from './Sidebar'
import { LayoutDashboard, ShoppingBag, ChefHat, Calendar, ListChecks } from 'lucide-react'

const mobileNavItems = [
  { path: '/dashboard', icon: LayoutDashboard, label: 'Home' },
  { path: '/pantry', icon: ShoppingBag, label: 'Pantry' },
  { path: '/recipes', icon: ChefHat, label: 'Recipes' },
  { path: '/meal-plan', icon: Calendar, label: 'Plan' },
  { path: '/shopping-list', icon: ListChecks, label: 'Shop' },
]

const Layout = () => {
  const location = useLocation()

  return (
    <div className="min-h-screen">
      <Navbar />
      <div className="flex">
        <Sidebar />
        <main className="flex-1 p-6 lg:p-8 min-h-[calc(100vh-4rem)] pb-24 md:pb-8 min-w-0 overflow-x-hidden">
          <Outlet />
        </main>
      </div>

      {/* Mobile bottom nav */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur-xl border-t border-slate-200 z-50 safe-area-pb">
        <div className="flex justify-around items-center h-16">
          {mobileNavItems.map((item) => {
            const Icon = item.icon
            const isActive = location.pathname === item.path
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex flex-col items-center justify-center flex-1 py-2 transition-colors ${
                  isActive ? 'text-primary-600' : 'text-slate-400'
                }`}
              >
                <Icon size={22} strokeWidth={isActive ? 2.5 : 2} />
                <span className="text-xs mt-0.5">{item.label}</span>
              </Link>
            )
          })}
        </div>
      </nav>
    </div>
  )
}

export default Layout
