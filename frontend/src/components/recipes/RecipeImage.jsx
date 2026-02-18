import { useState } from 'react'
import { ChefHat } from 'lucide-react'

/** Check if src is missing/invalid (show placeholder) */
const isInvalidSrc = (src) => {
  if (src == null) return true
  const s = String(src).trim()
  return s === '' || s === 'null' || s === 'undefined' || !s.startsWith('http')
}

/**
 * Recipe image with fallback when URL fails to load.
 * Prevents broken image icons and shows a styled placeholder.
 */
const RecipeImage = ({ src, alt, className = 'w-full h-48 object-cover' }) => {
  const [failed, setFailed] = useState(false)

  if (isInvalidSrc(src) || failed) {
    const sizeClass = className.includes('h-64') ? 'h-64' : 'h-48'
    const roundedClass = className.includes('rounded') ? 'rounded-lg' : ''
    return (
      <div
        className={`flex items-center justify-center bg-gradient-to-br from-primary-100 to-accent-100 w-full min-h-[12rem] ${sizeClass} ${roundedClass} ${className.includes('mb-6') ? 'mb-6' : ''}`}
      >
        <ChefHat className="w-16 h-16 text-primary-400" strokeWidth={1.5} />
      </div>
    )
  }

  return (
    <img
      src={src}
      alt={alt || 'Recipe'}
      className={className}
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={() => setFailed(true)}
    />
  )
}

export default RecipeImage
