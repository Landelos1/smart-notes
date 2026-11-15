import test from 'node:test'
import assert from 'node:assert/strict'
import { getUpcomingReviewDates } from '../src/utils/reviewSchedule.js'

test('daily review dates start today and stop at the plan end date', () => {
  const dates = getUpcomingReviewDates({
    frequency: 'daily', startDate: '2026-11-14', endDate: '2026-11-17'
  }, new Date(2026, 10, 15))

  assert.deepEqual(dates, ['2026-11-15', '2026-11-16', '2026-11-17'])
})

test('weekly review dates honor the selected ISO weekday', () => {
  const dates = getUpcomingReviewDates({
    frequency: 'weekly', repeatDay: 3, startDate: '2026-11-16', endDate: '2026-12-02'
  }, new Date(2026, 10, 17))

  assert.deepEqual(dates, ['2026-11-18', '2026-11-25', '2026-12-02'])
})

test('custom review dates keep their interval anchored to the start date', () => {
  const dates = getUpcomingReviewDates({
    frequency: 'custom', intervalDays: 3, startDate: '2026-11-15', endDate: '2026-11-24'
  }, new Date(2026, 10, 16))

  assert.deepEqual(dates, ['2026-11-18', '2026-11-21', '2026-11-24'])
})

test('invalid calendar dates are rejected instead of normalized', () => {
  assert.deepEqual(getUpcomingReviewDates({
    frequency: 'daily', startDate: '2026-02-31', endDate: '2026-03-05'
  }), [])
})

test('daily custom intervals remain calendar-based across daylight-saving changes', () => {
  const dates = getUpcomingReviewDates({
    frequency: 'custom', intervalDays: 1, startDate: '2026-10-31', endDate: '2026-11-03'
  }, new Date(2026, 10, 1))

  assert.deepEqual(dates, ['2026-11-01', '2026-11-02', '2026-11-03'])
})

test('invalid plans have no upcoming review dates', () => {
  assert.deepEqual(getUpcomingReviewDates({
    frequency: 'weekly', repeatDay: 9, startDate: '2026-12-02', endDate: '2026-11-16'
  }), [])
})
