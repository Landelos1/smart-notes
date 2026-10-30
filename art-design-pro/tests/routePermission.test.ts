import { test } from 'node:test'
import assert from 'node:assert/strict'
import { RoutePermissionValidator } from '../src/router/core/RoutePermissionValidator'
import type { AppRouteRecord } from '../src/types/router'

const routes = [
  { path: '/note', name: 'Notes', children: [
    { path: '/note/list', name: 'List' },
    { path: '/note/publish', name: 'Publish', meta: { isHide: true } }
  ] },
  { path: '/note-detail/:id', name: 'Detail', meta: { isHide: true } }
] as AppRouteRecord[]

test('hidden edit pages remain accessible after a refresh', () => {
  assert.equal(RoutePermissionValidator.hasPermission('/note/publish', routes), true)
})

test('detail parameters match one complete segment', () => {
  assert.equal(RoutePermissionValidator.hasPermission('/note-detail/123', routes), true)
  assert.equal(RoutePermissionValidator.hasPermission('/note-detail/123/delete', routes), false)
})

test('parent paths do not grant access to absent child routes', () => {
  assert.equal(RoutePermissionValidator.hasPermission('/note/restricted', routes), false)
  assert.deepEqual(RoutePermissionValidator.validatePath('/note/publish', [], '/dashboard'), {
    path: '/dashboard', hasPermission: false
  })
})
