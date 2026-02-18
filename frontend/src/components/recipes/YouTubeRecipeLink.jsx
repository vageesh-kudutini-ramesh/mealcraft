/**
 * YouTube link for recipes – opens search for dish name + recipe.
 * Iconic red YouTube-style button.
 */
const YouTubeRecipeLink = ({ recipeName, size = 'sm', className = '' }) => {
  const query = `${recipeName || ''} recipe cooking`.trim()
  const url = `https://www.youtube.com/results?search_query=${encodeURIComponent(query)}`

  const isSmall = size === 'sm'

  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      onClick={(e) => e.stopPropagation()}
      className={`inline-flex items-center gap-1.5 rounded-lg transition-all hover:scale-[1.02] active:scale-[0.98] ${className}`}
      title={`Watch ${recipeName} on YouTube`}
      aria-label={`Watch ${recipeName} recipe video on YouTube`}
    >
      <span
        className={`inline-flex items-center justify-center rounded ${isSmall ? 'w-6 h-6' : 'w-8 h-8'}`}
        style={{ backgroundColor: '#FF0000' }}
      >
        <svg
          viewBox="0 0 24 24"
          className={isSmall ? 'w-3.5 h-3.5' : 'w-5 h-5'}
          fill="white"
        >
          <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z" />
        </svg>
      </span>
      <span className={`font-medium text-red-600 ${isSmall ? 'text-xs' : 'text-sm'}`}>
        {isSmall ? 'Watch video' : 'Watch how to make'}
      </span>
    </a>
  )
}

export default YouTubeRecipeLink
