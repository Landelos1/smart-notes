const DAY_MS = 24 * 60 * 60 * 1000

function parseLocalDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value || ''))
  if (!match) return null
  const [year, month, day] = match.slice(1).map(Number)
  if (!year || !month || !day) return null
  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) return null
  return Number.isNaN(date.getTime()) ? null : date
}

function dayNumber(date) {
  return Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / DAY_MS
}

export function formatLocalDate(date) {
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-')
}

function isoWeekday(date) {
  return date.getDay() || 7
}

export function getUpcomingReviewDates(plan, from = new Date(), limit = 5) {
  const start = parseLocalDate(plan?.startDate)
  const end = parseLocalDate(plan?.endDate)
  if (!start || !end || end < start || limit < 1) return []

  const today = new Date(from.getFullYear(), from.getMonth(), from.getDate())
  let cursor = start > today ? start : today

  if (plan.frequency === 'weekly') {
    const day = Number(plan.repeatDay) || isoWeekday(start)
    while (cursor <= end && isoWeekday(cursor) !== day) cursor.setDate(cursor.getDate() + 1)
  } else if (plan.frequency === 'custom') {
    const interval = Math.max(1, Math.min(365, Number(plan.intervalDays) || 7))
    const elapsed = Math.max(0, dayNumber(cursor) - dayNumber(start))
    const offset = Math.ceil(elapsed / interval) * interval
    cursor = new Date(start)
    cursor.setDate(cursor.getDate() + offset)
  } else if (plan.frequency !== 'daily') {
    return []
  }

  const dates = []
  const step = plan.frequency === 'daily' ? 1 : plan.frequency === 'weekly' ? 7 : Number(plan.intervalDays) || 7
  while (cursor <= end && dates.length < limit) {
    dates.push(formatLocalDate(cursor))
    cursor.setDate(cursor.getDate() + step)
  }
  return dates
}
