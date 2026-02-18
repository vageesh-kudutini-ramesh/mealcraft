import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import axios from '../utils/axios'
import { useAuth } from '../contexts/AuthContext'
import { AlertTriangle, ShoppingBag, ChefHat, Calendar, ListChecks, Package, BookOpen, UtensilsCrossed, ArrowRight, User, LogOut } from 'lucide-react'
import SlidingTicker from '../components/common/SlidingTicker'
import ProfilePhotoAvatar from '../components/profile/ProfilePhotoAvatar'

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.08 } } }
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 } }

const Dashboard = () => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [stats, setStats] = useState(null)

  const profileMenuItems = [
    { icon: User, label: 'Profile', onClick: () => navigate('/profile') },
    { icon: LogOut, label: 'Logout', onClick: () => { logout(); navigate('/login') } },
  ]
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetch = async () => {
      try {
        const res = await axios.get('/api/dashboard/stats')
        setStats(res.data)
      } catch {
        setStats(null)
      } finally {
        setLoading(false)
      }
    }
    fetch()
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
          className="w-10 h-10 border-2 border-primary-500 border-t-transparent rounded-full"
        />
      </div>
    )
  }

  if (!stats) {
    return (
      <div className="text-center py-16 text-slate-600">
        <p>Unable to load dashboard. Please try again.</p>
      </div>
    )
  }

  const firstName = user?.fullName?.split(' ')[0] || 'there'

  const quickActions = [
    { path: '/recipes', icon: ChefHat, label: 'Suggest Recipes', desc: 'Find recipes using your pantry', color: 'primary' },
    { path: '/pantry', icon: ShoppingBag, label: 'View Pantry', desc: 'Manage your pantry items', color: 'accent' },
    { path: '/meal-plan', icon: Calendar, label: 'Plan Meals', desc: 'Schedule your weekly meals', color: 'primary' },
    { path: '/shopping-list', icon: ListChecks, label: 'Shopping List', desc: 'Items to buy', color: 'accent' },
  ]

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      {/* Hero */}
      <motion.section
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-primary-600 via-primary-500 to-primary-400 p-8 md:p-10 shadow-glow"
      >
        <div className="absolute inset-0 bg-grid-pattern bg-[size:32px_32px] opacity-10" />
        <div className="relative z-10 flex items-center gap-6 flex-wrap">
          <div className="ring-4 ring-white/30 rounded-2xl">
            <ProfilePhotoAvatar size="md" menuAlign="left" extraMenuItems={profileMenuItems} showUserInfo className="[&>button]:rounded-2xl [&>button]:ring-0" />
          </div>
          <div>
            <h1 className="text-2xl md:text-3xl font-display font-bold text-white mb-1">
              Hey, {firstName} 👋
            </h1>
            <p className="text-white/90 text-lg">What would you like to cook today?</p>
          </div>
        </div>
      </motion.section>

      <SlidingTicker variant="compact" />

      {/* Quick Actions */}
      <motion.section variants={container} initial="hidden" animate="show">
        <h2 className="text-lg font-display font-semibold text-slate-800 mb-4">Quick actions</h2>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {quickActions.map((action, i) => {
            const Icon = action.icon
            return (
              <motion.div key={action.path} variants={item}>
                <Link
                  to={action.path}
                  className="card-interactive flex flex-col items-center p-6 text-center group"
                >
                  <div className={`w-14 h-14 rounded-2xl flex items-center justify-center mb-3 ${
                    action.color === 'accent' ? 'bg-accent-100 text-accent-600 group-hover:bg-accent-200' : 'bg-primary-100 text-primary-600 group-hover:bg-primary-200'
                  } transition-colors`}>
                    <Icon size={28} />
                  </div>
                  <span className="font-semibold text-slate-800 mb-1">{action.label}</span>
                  <span className="text-xs text-slate-500">{action.desc}</span>
                  <ArrowRight className="w-4 h-4 mt-2 text-slate-400 opacity-0 group-hover:opacity-100 group-hover:translate-x-1 transition-all" />
                </Link>
              </motion.div>
            )
          })}
        </div>
      </motion.section>

      {/* Alerts */}
      <motion.section variants={container} initial="hidden" animate="show">
        <h2 className="text-lg font-display font-semibold text-slate-800 mb-4">Alerts & notifications</h2>
        <div className="grid md:grid-cols-3 gap-4">
          {/* Expiring Soon */}
          <motion.div variants={item}>
            {stats.expiringSoonCount > 0 ? (
              <div className="card p-5 border-l-4 border-amber-400 bg-amber-50/50">
                <div className="flex items-start gap-3">
                  <AlertTriangle className="text-amber-600 flex-shrink-0 mt-0.5" size={22} />
                  <div>
                    <h3 className="font-semibold text-amber-900">Items expiring soon</h3>
                    <p className="text-sm text-amber-800 mt-1">{stats.expiringSoonCount} need attention</p>
                    <ul className="text-sm text-amber-700 mt-2 space-y-1">
                      {stats.expiringSoonItems?.slice(0, 3).map((i) => (
                        <li key={i.id}>{i.itemName} – {i.daysUntilExpiry}d left</li>
                      ))}
                    </ul>
                    <Link to="/pantry?filter=expiring" className="text-amber-700 font-medium text-sm mt-2 inline-flex items-center gap-1 hover:underline">
                      View all <ArrowRight size={14} />
                    </Link>
                  </div>
                </div>
              </div>
            ) : (
              <div className="card p-5 border-l-4 border-emerald-400 bg-emerald-50/50">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-emerald-100 flex items-center justify-center">
                    <AlertTriangle className="text-emerald-600" size={20} />
                  </div>
                  <div>
                    <h3 className="font-semibold text-emerald-900">All good!</h3>
                    <p className="text-sm text-emerald-700">No items expiring soon</p>
                  </div>
                </div>
              </div>
            )}
          </motion.div>

          {/* Expired */}
          <motion.div variants={item}>
            {stats.expiredCount > 0 ? (
              <div className="card p-5 border-l-4 border-red-400 bg-red-50/50">
                <div className="flex items-start gap-3">
                  <AlertTriangle className="text-red-600 flex-shrink-0 mt-0.5" size={22} />
                  <div>
                    <h3 className="font-semibold text-red-900">Expired items</h3>
                    <p className="text-sm text-red-800">{stats.expiredCount} expired</p>
                    <Link to="/pantry?filter=expired" className="text-red-700 font-medium text-sm mt-2 inline-flex items-center gap-1 hover:underline">
                      View <ArrowRight size={14} />
                    </Link>
                  </div>
                </div>
              </div>
            ) : (
              <div className="card p-5 border-l-4 border-emerald-400 bg-emerald-50/50">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-emerald-100 flex items-center justify-center">
                    <AlertTriangle className="text-emerald-600" size={20} />
                  </div>
                  <div>
                    <h3 className="font-semibold text-emerald-900">No expired items</h3>
                    <p className="text-sm text-emerald-700">All fresh</p>
                  </div>
                </div>
              </div>
            )}
          </motion.div>

          {/* Low Stock */}
          <motion.div variants={item}>
            {stats.lowStockCount > 0 ? (
              <div className="card p-5 border-l-4 border-orange-400 bg-orange-50/50">
                <div className="flex items-start gap-3">
                  <AlertTriangle className="text-orange-600 flex-shrink-0 mt-0.5" size={22} />
                  <div>
                    <h3 className="font-semibold text-orange-900">Low stock</h3>
                    <p className="text-sm text-orange-800">{stats.lowStockCount} running low</p>
                    <Link to="/pantry?filter=low-stock" className="text-orange-700 font-medium text-sm mt-2 inline-flex items-center gap-1 hover:underline">
                      View <ArrowRight size={14} />
                    </Link>
                  </div>
                </div>
              </div>
            ) : (
              <div className="card p-5 border-l-4 border-emerald-400 bg-emerald-50/50">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-emerald-100 flex items-center justify-center">
                    <AlertTriangle className="text-emerald-600" size={20} />
                  </div>
                  <div>
                    <h3 className="font-semibold text-emerald-900">Stock levels good</h3>
                    <p className="text-sm text-emerald-700">All well stocked</p>
                  </div>
                </div>
              </div>
            )}
          </motion.div>
        </div>
      </motion.section>

      {/* Stats */}
      <motion.section variants={container} initial="hidden" animate="show">
        <h2 className="text-lg font-display font-semibold text-slate-800 mb-4">Overview</h2>
        <div className="grid md:grid-cols-3 gap-4">
          <motion.div variants={item} className="card p-6">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-12 h-12 rounded-xl bg-primary-100 flex items-center justify-center">
                <Package className="text-primary-600" size={24} />
              </div>
              <h3 className="font-semibold text-slate-800">Pantry items</h3>
            </div>
            <p className="text-3xl font-display font-bold text-primary-600">{stats.totalPantryItems}</p>
            <p className="text-sm text-slate-500 mt-1">Total in your pantry</p>
          </motion.div>
          <motion.div variants={item} className="card p-6">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-12 h-12 rounded-xl bg-accent-100 flex items-center justify-center">
                <BookOpen className="text-accent-600" size={24} />
              </div>
              <h3 className="font-semibold text-slate-800">Saved recipes</h3>
            </div>
            <p className="text-3xl font-display font-bold text-accent-600">{stats.savedRecipesCount}</p>
            <p className="text-sm text-slate-500 mt-1">In your collection</p>
          </motion.div>
          <motion.div variants={item} className="card p-6">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-12 h-12 rounded-xl bg-primary-100 flex items-center justify-center">
                <UtensilsCrossed className="text-primary-600" size={24} />
              </div>
              <h3 className="font-semibold text-slate-800">Today&apos;s meals</h3>
            </div>
            <p className="text-3xl font-display font-bold text-primary-600">{stats.todaysMealPlan?.length || 0}</p>
            <p className="text-sm text-slate-500 mt-1">Planned for today</p>
          </motion.div>
        </div>
      </motion.section>

      {/* Recent Recipes */}
      {stats.recentRecipes?.length > 0 && (
        <motion.section variants={container} initial="hidden" animate="show">
          <h2 className="text-lg font-display font-semibold text-slate-800 mb-4">Recently saved</h2>
          <div className="grid md:grid-cols-3 gap-4">
            {stats.recentRecipes.map((recipe, i) => (
              <motion.div key={recipe.id} variants={item}>
                <Link
                  to="/recipes"
                  className="card-interactive block overflow-hidden group"
                >
                  {recipe.imageUrl && (
                    <div className="aspect-video overflow-hidden">
                      <img
                        src={recipe.imageUrl}
                        alt={recipe.recipeName}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                        onError={(e) => { e.target.style.display = 'none' }}
                      />
                    </div>
                  )}
                  <div className="p-4">
                    <h3 className="font-semibold text-slate-800 group-hover:text-primary-600 transition-colors truncate">
                      {recipe.recipeName}
                    </h3>
                    <p className="text-sm text-slate-500 mt-1">
                      {recipe.prepTimeMinutes || 0} min prep · {recipe.cookTimeMinutes || 0} min cook
                    </p>
                    {recipe.matchPercentage != null && (
                      <p className="text-xs text-primary-600 mt-1 font-medium">
                        {Math.round(recipe.matchPercentage)}% pantry match
                      </p>
                    )}
                  </div>
                </Link>
              </motion.div>
            ))}
          </div>
        </motion.section>
      )}
    </div>
  )
}

export default Dashboard
