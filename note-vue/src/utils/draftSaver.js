/**
 * Debounce immutable drafts by note ID and serialize writes to the same note.
 * A failed write stays pending so the editor can retry it.
 * @template T
 * @param {(noteId: string, draft: T) => Promise<void>} save
 * @param {(noteId: string, state: 'pending'|'saving'|'saved'|'error') => void} onState
 * @param {number} delay
 */
export function createDraftSaver(save, onState = () => {}, delay = 1000) {
  const pending = new Map()
  const timers = new Map()
  const inFlight = new Map()
  const revisions = new Map()

  function schedule(noteId, draft) {
    const revision = (revisions.get(noteId) || 0) + 1
    revisions.set(noteId, revision)
    pending.set(noteId, { draft, revision })
    clearTimeout(timers.get(noteId))
    onState(noteId, 'pending')
    timers.set(noteId, setTimeout(() => { void flush(noteId).catch(() => {}) }, delay))
  }

  async function flush(noteId) {
    clearTimeout(timers.get(noteId))
    timers.delete(noteId)
    if (!pending.has(noteId)) return inFlight.get(noteId)

    const entry = pending.get(noteId)
    pending.delete(noteId)
    const previous = inFlight.get(noteId) || Promise.resolve()
    const write = previous.catch(() => {}).then(async () => {
      onState(noteId, 'saving')
      try {
        await save(noteId, entry.draft)
        if (revisions.get(noteId) === entry.revision) onState(noteId, 'saved')
      } catch (error) {
        // A newer draft may already be queued. Never restore an older failed write.
        if (revisions.get(noteId) === entry.revision) {
          if (!pending.has(noteId)) pending.set(noteId, entry)
          onState(noteId, 'error')
        }
        throw error
      }
    })
    inFlight.set(noteId, write)
    try {
      await write
    } finally {
      if (inFlight.get(noteId) === write) inFlight.delete(noteId)
    }
  }

  async function flushAll() {
    do {
      const results = await Promise.allSettled([...new Set([...pending.keys(), ...inFlight.keys()])].map(flush))
      const failure = results.find(result => result.status === 'rejected')
      if (failure) throw failure.reason
      // Include drafts typed while the preceding request was in flight.
    } while (pending.size > 0 || inFlight.size > 0)
  }

  return { schedule, flush, flushAll, hasUnsaved: () => pending.size > 0 || inFlight.size > 0 }
}
