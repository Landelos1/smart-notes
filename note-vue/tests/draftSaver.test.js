import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createDraftSaver } from '../src/utils/draftSaver.js'

const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

test('switching notes keeps each draft attached to its original ID', async () => {
  const writes = []
  const saver = createDraftSaver(async (id, draft) => { writes.push([id, draft]) }, undefined, 60000)
  saver.schedule('A', { content: 'first' })
  saver.schedule('B', { content: 'second' })
  await saver.flushAll()
  assert.deepEqual(writes, [['A', { content: 'first' }], ['B', { content: 'second' }]])
  assert.equal(saver.hasUnsaved(), false)
})

test('an older failed write cannot return and overwrite a newer queued draft', async () => {
  const first = deferred()
  const writes = []
  const saver = createDraftSaver(async (_, draft) => {
    writes.push(draft)
    if (draft === 'old') await first.promise
  }, undefined, 60000)
  saver.schedule('A', 'old')
  const older = saver.flush('A')
  const failed = assert.rejects(older, /offline/)
  await Promise.resolve()
  await Promise.resolve()
  saver.schedule('A', 'new')
  const newer = saver.flush('A')
  first.reject(new Error('offline'))
  await failed
  await newer
  await saver.flushAll()
  assert.deepEqual(writes, ['old', 'new'])
  assert.equal(saver.hasUnsaved(), false)
})

test('failed latest draft remains available for an explicit retry', async () => {
  let offline = true
  const writes = []
  const saver = createDraftSaver(async (_, draft) => {
    if (offline) throw new Error('offline')
    writes.push(draft)
  }, undefined, 60000)
  saver.schedule('A', 'latest')
  await assert.rejects(saver.flushAll(), /offline/)
  assert.equal(saver.hasUnsaved(), true)
  offline = false
  await saver.flushAll()
  assert.deepEqual(writes, ['latest'])
})

test('flushAll also saves edits entered during an in-flight request', async () => {
  const first = deferred()
  const writes = []
  const saver = createDraftSaver(async (_, draft) => {
    writes.push(draft)
    if (draft === 'first') await first.promise
  }, undefined, 60000)
  saver.schedule('A', 'first')
  const finished = saver.flushAll()
  await Promise.resolve()
  await Promise.resolve()
  saver.schedule('A', 'last')
  first.resolve()
  await finished
  assert.deepEqual(writes, ['first', 'last'])
  assert.equal(saver.hasUnsaved(), false)
})
