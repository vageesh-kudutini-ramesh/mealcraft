import { useMemo } from 'react'
import { MARQUEE_ITEMS } from '../../data/marqueeContent'

/**
 * Sliding ticker – smooth infinite marquee, content flows left-to-right (exits right, enters left).
 * Used in navbar and empty states for dynamic, engaging content.
 */
const SlidingTicker = ({ variant = 'default', items = MARQUEE_ITEMS }) => {
  const shuffled = useMemo(() => {
    const arr = [...items]
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]]
    }
    return arr
  }, [items])

  // Triple the content for seamless infinite loop – no harsh cutoffs
  const tickerItems = [...shuffled, ...shuffled, ...shuffled]

  const isCompact = variant === 'compact' || variant === 'navbar'
  const isNavbar = variant === 'navbar'

  return (
    <div
      className={`relative overflow-hidden select-none w-full ${
        isCompact ? 'py-2' : 'py-3'
      } ${
        isNavbar
          ? 'bg-gradient-to-r from-primary-100/80 via-primary-50/90 to-accent-50/70 border-y border-primary-200/60'
          : 'bg-gradient-to-r from-primary-50/80 via-accent-50/50 to-primary-50/80 border-y border-primary-100/60'
      }`}
      style={{
        maskImage: 'linear-gradient(to right, transparent 0%, black 8%, black 92%, transparent 100%)',
        WebkitMaskImage: 'linear-gradient(to right, transparent 0%, black 8%, black 92%, transparent 100%)',
      }}
    >
      <div
        className="flex items-center"
        style={{
          width: 'max-content',
          animation: `marquee ${isNavbar ? 620 : 720}s linear infinite`,
        }}
      >
        {tickerItems.map((item, i) => (
          <div
            key={`${item.text}-${i}`}
            className={`flex items-center gap-2 shrink-0 mx-5 whitespace-nowrap ${
              isCompact ? 'text-slate-600' : 'text-slate-700'
            }`}
          >
            <span className="text-base">{item.icon}</span>
            <span className={isCompact ? 'text-xs sm:text-sm font-medium' : 'text-sm font-medium'}>
              {item.text}
            </span>
            <span className="text-primary-400 font-bold text-sm">•</span>
          </div>
        ))}
      </div>
    </div>
  )
}

export default SlidingTicker
