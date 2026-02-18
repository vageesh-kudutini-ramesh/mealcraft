import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { LogOut, User } from 'lucide-react'
import SlidingTicker from '../common/SlidingTicker'
import NotificationBell from './NotificationBell'
import ProfilePhotoAvatar from '../profile/ProfilePhotoAvatar'

const Navbar = () => {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const extraMenuItems = [
    { icon: User, label: 'Profile', onClick: () => navigate('/profile') },
    { icon: LogOut, label: 'Logout', onClick: handleLogout },
  ]

  return (
    <nav className="bg-white/80 backdrop-blur-xl border-b border-slate-200/80 sticky top-0 z-40">
      <div className="max-w-[1600px] mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16 min-h-[4rem]">
          <Link to="/dashboard" className="flex items-center gap-2 group">
            <span className="text-xl font-display font-bold text-primary-600 group-hover:text-primary-700 transition-colors">
              MealCraft
            </span>
            <span className="hidden sm:inline text-sm text-slate-500">Plan smarter. Eat better.</span>
          </Link>

          <div className="flex items-center gap-3">
            <NotificationBell />

            <div className="relative">
              <ProfilePhotoAvatar
                size="sm"
                extraMenuItems={extraMenuItems}
                showUserInfo
                className="shrink-0"
              />
            </div>
          </div>
        </div>
      </div>
      {/* Full-width sliding ticker – spans edge to edge */}
      <div className="w-full overflow-hidden">
        <SlidingTicker variant="navbar" />
      </div>
    </nav>
  )
}

export default Navbar
