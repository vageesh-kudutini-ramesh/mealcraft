import { useState, useRef } from 'react'
import { createPortal } from 'react-dom'
import { useAuth } from '../../contexts/AuthContext'

const getProfileColor = (name) => {
  if (!name) return '#0284c7'
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return `hsl(${Math.abs(hash % 360)}, 65%, 50%)`
}

/**
 * Simple initials avatar with dropdown menu (Profile, Logout).
 * Used in Navbar, Dashboard, and Profile page.
 */
const ProfilePhotoAvatar = ({
  size = 'md',
  className = '',
  extraMenuItems = [],
  showUserInfo = false,
  menuAlign = 'right',
}) => {
  const { user } = useAuth()
  const [showMenu, setShowMenu] = useState(false)
  const [menuPos, setMenuPos] = useState(null)
  const btnRef = useRef(null)

  const sizeClasses = {
    sm: 'w-9 h-9 text-sm',
    md: 'w-16 h-16 text-xl',
    lg: 'w-28 h-28 text-3xl',
  }
  const s = sizeClasses[size] || sizeClasses.md
  const hasMenu = extraMenuItems && extraMenuItems.length > 0

  const handleToggle = () => {
    if (!hasMenu) return
    if (showMenu) {
      setShowMenu(false)
      setMenuPos(null)
    } else {
      const r = btnRef.current?.getBoundingClientRect()
      if (r) setMenuPos({ top: r.bottom + 6, left: r.left, right: window.innerWidth - r.right })
      setShowMenu(true)
    }
  }

  return (
    <div className={`relative ${className}`}>
      <button
        ref={btnRef}
        type="button"
        onClick={handleToggle}
        className={`${s} rounded-2xl flex items-center justify-center border-2 border-white shadow-md hover:ring-2 hover:ring-primary-400 transition-all flex-shrink-0 ${
          hasMenu ? 'cursor-pointer' : 'cursor-default'
        }`}
        style={{ backgroundColor: getProfileColor(user?.fullName) }}
      >
        <span className="text-white font-semibold">{user?.initials || 'U'}</span>
      </button>

      {showMenu && hasMenu && menuPos &&
        createPortal(
          <>
            <div
              className="fixed inset-0 z-[99]"
              onClick={() => { setShowMenu(false); setMenuPos(null) }}
              aria-hidden="true"
            />
            <div
              className="fixed w-52 bg-white rounded-xl shadow-lg border border-slate-200 py-1 z-[100]"
              style={{
                top: menuPos.top,
                ...(menuAlign === 'left' ? { left: menuPos.left } : { right: menuPos.right }),
              }}
            >
              {showUserInfo && (
                <div className="px-4 py-2 border-b border-slate-100">
                  <p className="font-medium text-slate-900 truncate text-sm">{user?.fullName || 'User'}</p>
                  <p className="text-xs text-slate-500 truncate">{user?.email || ''}</p>
                </div>
              )}
              {extraMenuItems.map((item, i) => {
                const Icon = item.icon
                return (
                  <button
                    key={i}
                    type="button"
                    onClick={() => {
                      setShowMenu(false)
                      setMenuPos(null)
                      item.onClick?.()
                    }}
                    className="flex items-center gap-3 w-full px-4 py-2.5 text-slate-700 hover:bg-slate-50 transition-colors text-left text-sm"
                  >
                    {Icon && <Icon size={18} className="flex-shrink-0" />}
                    <span>{item.label}</span>
                  </button>
                )
              })}
            </div>
          </>,
          document.body
        )}
    </div>
  )
}

export default ProfilePhotoAvatar
