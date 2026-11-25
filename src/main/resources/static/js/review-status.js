function isPendingReviewStatus(status) {
  return ['DIHANTAR', 'SEMAKAN_STAFF'].includes(String(status || '').toUpperCase());
}

function isKppReturnedApplication(item) {
  return Boolean(
    item
    && String(item.status || '').toUpperCase() === 'DIHANTAR'
    && String(item.kppNote || '').trim()
  );
}

function getReviewCounts(applications) {
  const pending = applications.filter((item) => isPendingReviewStatus(item.status));
  const reviewed = pending.filter(isKppReturnedApplication).length;
  return { pending: pending.length - reviewed, reviewed };
}
