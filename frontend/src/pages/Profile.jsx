import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from '../utils/axios'
import { useAuth } from '../contexts/AuthContext'
import { useNotification } from '../contexts/NotificationContext'
import ProfilePhotoAvatar from '../components/profile/ProfilePhotoAvatar'
import { User, LogOut } from 'lucide-react'

/**
 * Profile Page - User profile management with editable fields.
 */
const Profile = () => {
  const { updateProfile, logout } = useAuth()
  const navigate = useNavigate()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)

  const profileMenuItems = [
    { icon: User, label: 'Profile', onClick: () => navigate('/profile') },
    { icon: LogOut, label: 'Logout', onClick: () => { logout(); navigate('/login') } },
  ]
  const [editing, setEditing] = useState(false)
  const [formData, setFormData] = useState({})
  const { showSuccess, showError } = useNotification()

  useEffect(() => {
    fetchProfile()
  }, [])

  const fetchProfile = async () => {
    try {
      const response = await axios.get('/api/users/profile')
      setProfile(response.data)
      setFormData(response.data)
    } catch (error) {
      showError('Error loading profile')
    } finally {
      setLoading(false)
    }
  }

  const handleSave = async () => {
    try {
      await updateProfile(formData)
      setProfile(formData)
      setEditing(false)
      showSuccess('Profile updated successfully!')
    } catch (error) {
      showError('Error updating profile')
    }
  }

  const getProfileColor = (name) => {
    if (!name) return '#6366f1'
    let hash = 0
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash)
    }
    const hue = Math.abs(hash % 360)
    return `hsl(${hue}, 70%, 50%)`
  }

  if (loading) {
    return <div className="text-center py-12">Loading profile...</div>
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <h1 className="text-3xl font-bold text-gray-900">Profile</h1>

      <div className="bg-white rounded-lg shadow p-6">
        {/* Profile Avatar - initials with Profile/Logout menu */}
        <div className="flex justify-center mb-6">
          <ProfilePhotoAvatar size="lg" extraMenuItems={profileMenuItems} showUserInfo menuAlign="left" />
        </div>

        {/* Profile Form */}
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Email</label>
            <input
              type="email"
              value={profile?.email || ''}
              disabled
              className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-900"
            />
            <p className="text-xs text-gray-500 mt-1">Email cannot be changed</p>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">First Name</label>
              <input
                type="text"
                value={editing ? formData.firstName : profile?.firstName || ''}
                onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                disabled={!editing}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg disabled:bg-gray-50 text-gray-900"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Last Name</label>
              <input
                type="text"
                value={editing ? formData.lastName : profile?.lastName || ''}
                onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                disabled={!editing}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg disabled:bg-gray-50 text-gray-900"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Age (Optional)</label>
            <input
              type="number"
              value={editing ? formData.age || '' : profile?.age || ''}
              onChange={(e) => setFormData({ ...formData, age: e.target.value ? parseInt(e.target.value) : null })}
              disabled={!editing}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg disabled:bg-gray-50 text-gray-900"
            />
          </div>

          <div className="flex space-x-4 pt-4">
            {editing ? (
              <>
                <button
                  onClick={handleSave}
                  className="flex-1 bg-primary-600 text-white py-2 px-4 rounded-lg hover:bg-primary-700"
                >
                  Save Changes
                </button>
                <button
                  onClick={() => {
                    setEditing(false)
                    setFormData(profile)
                  }}
                  className="flex-1 bg-gray-200 text-gray-700 py-2 px-4 rounded-lg hover:bg-gray-300"
                >
                  Cancel
                </button>
              </>
            ) : (
              <button
                onClick={() => setEditing(true)}
                className="flex-1 bg-primary-600 text-white py-2 px-4 rounded-lg hover:bg-primary-700"
              >
                Edit Profile
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default Profile



