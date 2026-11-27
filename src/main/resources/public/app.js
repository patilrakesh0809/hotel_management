const state = {
  summary: null,
  rooms: [],
  reservations: [],
  extrasCatalog: [],
  roomStatusFilter: 'ALL',
  roomTypeFilter: 'ALL',
  resStatusFilter: 'ALL',
  resSearch: '',
  lastCalculatedBill: null,
};

const $ = (id) => document.getElementById(id);

function money(val) {
  return '$' + Number(val || 0).toFixed(2);
}

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function showToast(msg, isError = false) {
  const toast = $('toast');
  toast.textContent = msg;
  toast.className = isError ? 'toast error' : 'toast';
  toast.hidden = false;
  clearTimeout(showToast._timer);
  showToast._timer = setTimeout(() => {
    toast.hidden = true;
  }, 3600);
}

async function api(url, options = {}) {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  });
  const text = await res.text();
  let data = {};
  try {
    data = text ? JSON.parse(text) : {};
  } catch {
    data = { raw: text };
  }
  if (!res.ok) {
    throw new Error(data.error || `Request failed (${res.status})`);
  }
  return data;
}

function todayISO(offsetDays = 0) {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  return d.toISOString().slice(0, 10);
}

function switchTab(tabName) {
  document.querySelectorAll('.tab').forEach((btn) => {
    const active = btn.dataset.tab === tabName;
    btn.classList.toggle('active', active);
    btn.setAttribute('aria-selected', String(active));
  });
  document.querySelectorAll('.tab-panel').forEach((panel) => {
    panel.classList.toggle('active', panel.id === `panel-${tabName}`);
  });
  if (tabName === 'empty') loadEmptyRooms();
  if (tabName === 'records') loadJsonPreview();
}

async function refreshAll() {
  const [summary, roomsRes, resRes] = await Promise.all([
    api('/api/summary'),
    api(`/api/rooms?status=${encodeURIComponent(state.roomStatusFilter)}&type=${encodeURIComponent(state.roomTypeFilter)}`),
    api(`/api/reservations?status=${encodeURIComponent(state.resStatusFilter)}&q=${encodeURIComponent(state.resSearch)}`),
  ]);

  state.summary = summary;
  state.rooms = roomsRes.rooms || [];
  state.reservations = resRes.reservations || [];
  state.extrasCatalog = summary.extrasCatalog || [];

  renderSummary();
  renderRooms();
  renderReservations();
  populateRoomSelectors();
  renderExtrasCheckboxes('calcExtrasList', 'calc');
  renderExtrasCheckboxes('resExtrasList', 'res');
  await loadEmptyRooms();
  await calculateStandaloneBill();
}

function renderSummary() {
  const s = state.summary;
  if (!s) return;
  $('kpiTotalRooms').textContent = s.totalRooms;
  $('kpiAvailableRooms').textContent = s.availableRooms;
  $('kpiOccupancyRate').textContent = `${s.occupancyPercent}% occupied`;
  $('kpiReservedRooms').textContent = s.reservedRooms;
  $('kpiOccupiedRooms').textContent = s.occupiedRooms;
  $('kpiRevenue').textContent = money(s.collectedRevenue);
  $('kpiPendingRevenue').textContent = `${money(s.pendingRevenue)} active stays`;
  $('availabilityPillText').textContent = `${s.availableRooms} / ${s.totalRooms} Empty Rooms`;
  $('jsonFilePath').textContent = s.jsonFilePath || 'data/hotel-records.json';
  $('csvFilePath').textContent = s.csvFilePath || 'data/reservations-log.csv';
  $('lastSavedAt').textContent = s.lastSavedAt || 'Just now';
}

function renderRooms() {
  const grid = $('roomsGrid');
  if (!state.rooms.length) {
    grid.innerHTML = '<div class="card">No rooms match the selected filter.</div>';
    return;
  }
  grid.innerHTML = state.rooms.map((room) => roomCardHtml(room)).join('');
}

function roomCardHtml(room, compactEmpty = false) {
  const active = room.activeReservation;
  const amenities = (room.amenities || []).map((a) => `<span class="amenity-tag">${escapeHtml(a)}</span>`).join('');
  let guestHtml = '';
  let actionsHtml = '';

  if (room.status === 'AVAILABLE') {
    actionsHtml = `
      <button class="btn btn-primary btn-sm" type="button" onclick="openBookingModal(${room.roomNumber})">Reserve Room</button>
      <button class="btn btn-secondary btn-sm" type="button" onclick="jumpToCalculator(${room.roomNumber})">Estimate Bill</button>
    `;
  } else if (active) {
    guestHtml = `
      <div class="room-guest-box">
        <strong>${escapeHtml(active.guestName)} (${escapeHtml(active.id)})</strong>
        <div>${escapeHtml(active.checkInDate)} → ${escapeHtml(active.checkOutDate)} (${active.nights} nights)</div>
        <div>Total Bill: <strong>${money(active.bill?.totalAmount)}</strong></div>
      </div>
    `;
    if (active.status === 'RESERVED') {
      actionsHtml = `
        <button class="btn btn-success btn-sm" type="button" onclick="doCheckIn('${escapeHtml(active.id)}')">Check In</button>
        <button class="btn btn-secondary btn-sm" type="button" onclick="openEditModal('${escapeHtml(active.id)}')">Edit</button>
        <button class="btn btn-secondary btn-sm" type="button" onclick="viewInvoice('${escapeHtml(active.id)}')">Bill</button>
        <button class="btn btn-danger btn-sm" type="button" onclick="doCancel('${escapeHtml(active.id)}')">Cancel</button>
      `;
    } else if (active.status === 'CHECKED_IN') {
      actionsHtml = `
        <button class="btn btn-primary btn-sm" type="button" onclick="doCheckOut('${escapeHtml(active.id)}')">Check Out &amp; Pay</button>
        <button class="btn btn-secondary btn-sm" type="button" onclick="openEditModal('${escapeHtml(active.id)}')">Edit / Add Extras</button>
        <button class="btn btn-secondary btn-sm" type="button" onclick="viewInvoice('${escapeHtml(active.id)}')">View Bill</button>
      `;
    }
  }

  return `
    <article class="room-card">
      <div>
        <div class="room-card-top">
          <div>
            <div class="room-number">Room ${room.roomNumber}</div>
            <div class="room-type">${escapeHtml(room.type)} · Max ${room.maxGuests} guests</div>
          </div>
          <span class="status-badge status-${escapeHtml(room.status)}">${escapeHtml(room.status)}</span>
        </div>
        <div class="room-rate" style="margin: 10px 0 8px;">${money(room.nightlyRate)} <span>/ night</span></div>
        <div class="amenities-list">${amenities}</div>
      </div>
      ${guestHtml}
      <div class="room-actions">${actionsHtml}</div>
    </article>
  `;
}

async function loadEmptyRooms() {
  const type = $('emptyTypeFilter').value;
  const minGuests = $('emptyGuestsFilter').value;
  const maxRate = $('emptyMaxRateFilter').value;
  const params = new URLSearchParams();
  if (type && type !== 'ALL') params.set('type', type);
  if (minGuests) params.set('minGuests', minGuests);
  if (maxRate) params.set('maxRate', maxRate);

  const data = await api(`/api/rooms/empty?${params.toString()}`);
  const empty = data.emptyRooms || [];
  $('emptySummaryText').textContent = `${empty.length} matching empty room(s) available right now (out of ${data.totalRooms} total rooms).`;
  const grid = $('emptyRoomsGrid');
  if (!empty.length) {
    grid.innerHTML = '<div class="card">No empty rooms match your criteria. Try widening the filters.</div>';
    return;
  }
  grid.innerHTML = empty.map((room) => roomCardHtml(room, true)).join('');
}

function renderReservations() {
  const tbody = $('reservationsTableBody');
  if (!state.reservations.length) {
    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:#64748b;padding:24px;">No reservation records found yet. Click "+ New Reservation" to book a room.</td></tr>';
    return;
  }

  tbody.innerHTML = state.reservations.map((r) => {
    let actions = `<button class="btn btn-secondary btn-sm" type="button" onclick="viewInvoice('${escapeHtml(r.id)}')">Invoice</button>`;
    if (r.status === 'RESERVED') {
      actions = `
        <button class="btn btn-success btn-sm" type="button" onclick="doCheckIn('${escapeHtml(r.id)}')">Check In</button>
        <button class="btn btn-secondary btn-sm" type="button" onclick="openEditModal('${escapeHtml(r.id)}')">Edit</button>
        ${actions}
        <button class="btn btn-danger btn-sm" type="button" onclick="doCancel('${escapeHtml(r.id)}')">Cancel</button>
      `;
    } else if (r.status === 'CHECKED_IN') {
      actions = `
        <button class="btn btn-primary btn-sm" type="button" onclick="doCheckOut('${escapeHtml(r.id)}')">Check Out</button>
        <button class="btn btn-secondary btn-sm" type="button" onclick="openEditModal('${escapeHtml(r.id)}')">Edit</button>
        ${actions}
      `;
    }
    return `
      <tr>
        <td><strong>${escapeHtml(r.id)}</strong></td>
        <td>Room ${r.roomNumber}<br><span class="muted">${escapeHtml(r.bill?.roomType || '')}</span></td>
        <td><strong>${escapeHtml(r.guestName)}</strong><br><span class="muted">${escapeHtml(r.guestEmail || r.guestPhone || '—')}</span></td>
        <td>${escapeHtml(r.checkInDate)}<br><span class="muted">to ${escapeHtml(r.checkOutDate)}</span></td>
        <td>${r.nights} night(s)<br><span class="muted">${r.guests} guest(s)</span></td>
        <td><strong>${money(r.bill?.totalAmount)}</strong><br><span class="muted">${r.paid ? 'PAID' : 'UNPAID'}</span></td>
        <td><span class="status-badge status-${escapeHtml(r.status)}">${escapeHtml(r.status)}</span></td>
        <td><div class="room-actions">${actions}</div></td>
      </tr>
    `;
  }).join('');
}

function populateRoomSelectors() {
  const calcSelect = $('calcRoomSelect');
  const prevCalc = calcSelect.value;
  const allRooms = state.rooms.length ? state.rooms : [];
  // Fetch all rooms list cleanly from summary/api if filtered
  api('/api/rooms?status=ALL').then((data) => {
    const rooms = data.rooms || [];
    calcSelect.innerHTML = rooms.map((r) =>
      `<option value="${r.roomNumber}">Room ${r.roomNumber} — ${escapeHtml(r.type)} (${money(r.nightlyRate)}/night)</option>`
    ).join('');
    if (prevCalc && rooms.some((r) => String(r.roomNumber) === prevCalc)) {
      calcSelect.value = prevCalc;
    }

    const resSelect = $('resRoomSelect');
    const prevRes = resSelect.value;
    resSelect.innerHTML = rooms.map((r) => {
      const availLabel = r.status === 'AVAILABLE' ? 'Available' : r.status;
      return `<option value="${r.roomNumber}">Room ${r.roomNumber} — ${escapeHtml(r.type)} (${money(r.nightlyRate)}/night) [${availLabel}]</option>`;
    }).join('');
    if (prevRes) resSelect.value = prevRes;
  });
}

function renderExtrasCheckboxes(containerId, prefix) {
  const container = $(containerId);
  if (!container || container.dataset.populated === '1') return;
  if (!state.extrasCatalog.length) return;

  container.innerHTML = state.extrasCatalog.map((item) => `
    <label class="extra-option">
      <input type="checkbox" name="${prefix}-extra" value="${escapeHtml(item.code)}">
      <span>${escapeHtml(item.label)} (${money(item.price)}${item.perNight ? '/night' : ' flat'})</span>
    </label>
  `).join('');
  container.dataset.populated = '1';

  if (prefix === 'res') {
    container.querySelectorAll('input').forEach((cb) => cb.addEventListener('change', updateModalBillPreview));
  } else {
    container.querySelectorAll('input').forEach((cb) => cb.addEventListener('change', calculateStandaloneBill));
  }
}

function selectedExtras(prefix) {
  return Array.from(document.querySelectorAll(`input[name="${prefix}-extra"]:checked`)).map((el) => el.value);
}

function renderBillHtml(bill) {
  if (!bill) return '<p class="muted">No bill calculated.</p>';
  const extrasRows = (bill.extraItems || []).map((item) => `
    <div class="invoice-line">
      <span>${escapeHtml(item.label)} (${item.quantity} × ${money(item.unitPrice)})</span>
      <span>${money(item.amount)}</span>
    </div>
  `).join('');

  const customRow = bill.customFee > 0 ? `
    <div class="invoice-line">
      <span>${escapeHtml(bill.customFeeLabel || 'Custom Fee')}</span>
      <span>${money(bill.customFee)}</span>
    </div>
  ` : '';

  const discountRow = bill.discountAmount > 0 ? `
    <div class="invoice-line" style="color:#065f46;">
      <span>Discount (${bill.discountPercent}%${bill.promoCode ? ' · ' + escapeHtml(bill.promoCode) : ''})</span>
      <span>-${money(bill.discountAmount)}</span>
    </div>
  ` : '';

  return `
    <div class="invoice-lines">
      <div class="invoice-line">
        <span>Room ${bill.roomNumber} (${escapeHtml(bill.roomType)}) · ${bill.nights} night(s) × ${money(bill.nightlyRate)}</span>
        <span>${money(bill.roomCharge)}</span>
      </div>
      ${extrasRows}
      ${customRow}
      <div class="invoice-divider"></div>
      <div class="invoice-line">
        <strong>Subtotal</strong>
        <strong>${money(bill.subtotal)}</strong>
      </div>
      ${discountRow}
      <div class="invoice-line">
        <span>Hospitality Tax (${Math.round((bill.taxRate || 0.12) * 100)}%)</span>
        <span>${money(bill.taxAmount)}</span>
      </div>
      <div class="invoice-total">
        <span>Grand Total</span>
        <span>${money(bill.totalAmount)}</span>
      </div>
    </div>
  `;
}

async function calculateStandaloneBill() {
  const roomNumber = Number($('calcRoomSelect').value || 101);
  const nights = Math.max(1, Number($('calcNights').value || 1));
  const payload = {
    roomNumber,
    nights,
    extras: selectedExtras('calc'),
    customFee: Number($('calcCustomFee').value || 0),
    customFeeLabel: $('calcCustomFeeLabel').value,
    discountPercent: Number($('calcDiscount').value || 0),
    promoCode: $('calcPromo').value,
  };
  try {
    const data = await api('/api/bills/calculate', { method: 'POST', body: JSON.stringify(payload) });
    state.lastCalculatedBill = data.bill;
    $('calcRoomSubtitle').textContent = `Room ${data.bill.roomNumber} · ${data.bill.roomType} · ${data.bill.nights} night(s)`;
    $('calcInvoiceBody').innerHTML = renderBillHtml(data.bill);
  } catch (err) {
    $('calcInvoiceBody').innerHTML = `<p style="color:#9f1239;">${escapeHtml(err.message)}</p>`;
  }
}

async function openBookingModal(prefillRoom = null, prefillCalc = false) {
  $('resEditId').value = '';
  $('resModalTitle').textContent = 'New Room Reservation';
  $('submitResBtn').textContent = 'Confirm Reservation';
  $('resRoomSelect').disabled = false;
  $('immediateCheckInWrap').style.display = 'flex';
  $('resImmediateCheckIn').checked = false;

  const allRoomsData = await api('/api/rooms?status=ALL');
  const rooms = allRoomsData.rooms || [];
  const availableRooms = rooms.filter((r) => r.status === 'AVAILABLE');
  const chosen = prefillRoom || (availableRooms[0]?.roomNumber ?? 101);

  $('resRoomSelect').innerHTML = rooms.map((r) =>
    `<option value="${r.roomNumber}" ${r.status !== 'AVAILABLE' ? 'disabled' : ''}>Room ${r.roomNumber} — ${escapeHtml(r.type)} (${money(r.nightlyRate)}/night) [${r.status}]</option>`
  ).join('');
  $('resRoomSelect').value = String(chosen);

  $('resGuestName').value = '';
  $('resGuestPhone').value = '';
  $('resGuestEmail').value = '';
  $('resGuests').value = 1;
  $('resCheckIn').value = todayISO(0);
  $('resCheckOut').value = todayISO(prefillCalc ? Number($('calcNights').value || 1) : 2);
  $('resCustomFee').value = prefillCalc ? $('calcCustomFee').value : '0';
  $('resDiscount').value = prefillCalc ? $('calcDiscount').value : '0';
  $('resPromoCode').value = prefillCalc ? $('calcPromo').value : '';
  $('resNotes').value = '';

  const calcExtras = prefillCalc ? selectedExtras('calc') : [];
  document.querySelectorAll('input[name="res-extra"]').forEach((cb) => {
    cb.checked = calcExtras.includes(cb.value);
  });

  $('reservationModal').hidden = false;
  await updateModalBillPreview();
}

async function openEditModal(id) {
  const res = state.reservations.find((r) => r.id === id);
  if (!res) return;

  $('resEditId').value = res.id;
  $('resModalTitle').textContent = `Edit Reservation ${res.id} (Room ${res.roomNumber})`;
  $('submitResBtn').textContent = 'Save Changes';
  $('immediateCheckInWrap').style.display = 'none';

  const allRoomsData = await api('/api/rooms?status=ALL');
  const rooms = allRoomsData.rooms || [];
  $('resRoomSelect').innerHTML = rooms.map((r) =>
    `<option value="${r.roomNumber}">Room ${r.roomNumber} — ${escapeHtml(r.type)}</option>`
  ).join('');
  $('resRoomSelect').value = String(res.roomNumber);
  $('resRoomSelect').disabled = true;

  $('resGuestName').value = res.guestName || '';
  $('resGuestPhone').value = res.guestPhone || '';
  $('resGuestEmail').value = res.guestEmail || '';
  $('resGuests').value = res.guests || 1;
  $('resCheckIn').value = res.checkInDate || todayISO(0);
  $('resCheckOut').value = res.checkOutDate || todayISO(1);
  $('resCustomFee').value = res.customFee || 0;
  $('resDiscount').value = res.discountPercent || 0;
  $('resPromoCode').value = res.promoCode || '';
  $('resNotes').value = res.notes || '';

  const chosenExtras = res.extras || [];
  document.querySelectorAll('input[name="res-extra"]').forEach((cb) => {
    cb.checked = chosenExtras.includes(cb.value);
  });

  $('reservationModal').hidden = false;
  await updateModalBillPreview();
}

function computeModalNights() {
  const inVal = $('resCheckIn').value;
  const outVal = $('resCheckOut').value;
  if (!inVal || !outVal) return 1;
  const diff = Math.round((new Date(outVal) - new Date(inVal)) / 86400000);
  return diff >= 1 ? diff : 1;
}

async function updateModalBillPreview() {
  const payload = {
    roomNumber: Number($('resRoomSelect').value || 101),
    nights: computeModalNights(),
    extras: selectedExtras('res'),
    customFee: Number($('resCustomFee').value || 0),
    discountPercent: Number($('resDiscount').value || 0),
    promoCode: $('resPromoCode').value,
  };
  try {
    const data = await api('/api/bills/calculate', { method: 'POST', body: JSON.stringify(payload) });
    $('resBillPreview').innerHTML = `<strong style="display:block;margin-bottom:8px;">Estimated Stay Bill (${data.bill.nights} night(s))</strong>` + renderBillHtml(data.bill);
  } catch (err) {
    $('resBillPreview').innerHTML = `<span style="color:#9f1239;">${escapeHtml(err.message)}</span>`;
  }
}

async function submitReservationForm(e) {
  e.preventDefault();
  const editId = $('resEditId').value;
  const payload = {
    roomNumber: Number($('resRoomSelect').value),
    guestName: $('resGuestName').value.trim(),
    guestPhone: $('resGuestPhone').value.trim(),
    guestEmail: $('resGuestEmail').value.trim(),
    guests: Number($('resGuests').value),
    checkInDate: $('resCheckIn').value,
    checkOutDate: $('resCheckOut').value,
    extras: selectedExtras('res'),
    customFee: Number($('resCustomFee').value || 0),
    discountPercent: Number($('resDiscount').value || 0),
    promoCode: $('resPromoCode').value.trim(),
    notes: $('resNotes').value.trim(),
    immediateCheckIn: $('resImmediateCheckIn').checked,
  };

  try {
    if (editId) {
      await api(`/api/reservations/${encodeURIComponent(editId)}`, { method: 'PUT', body: JSON.stringify(payload) });
      showToast(`Updated reservation ${editId} and saved to file.`);
    } else {
      const result = await api('/api/reservations', { method: 'POST', body: JSON.stringify(payload) });
      showToast(`Booked Room ${payload.roomNumber} (${result.reservation.id}) and saved to file.`);
    }
    $('reservationModal').hidden = true;
    await refreshAll();
  } catch (err) {
    showToast(err.message, true);
  }
}

async function doCheckIn(id) {
  try {
    await api(`/api/reservations/${encodeURIComponent(id)}/checkin`, { method: 'POST', body: '{}' });
    showToast(`Guest checked in (${id}).`);
    await refreshAll();
  } catch (err) {
    showToast(err.message, true);
  }
}

async function doCheckOut(id) {
  try {
    const res = await api(`/api/reservations/${encodeURIComponent(id)}/checkout`, { method: 'POST', body: '{}' });
    showToast(`Checked out ${id} · Total Paid: ${money(res.reservation.bill?.totalAmount)}.`);
    await refreshAll();
    viewInvoice(id);
  } catch (err) {
    showToast(err.message, true);
  }
}

async function doCancel(id) {
  try {
    await api(`/api/reservations/${encodeURIComponent(id)}/cancel`, { method: 'POST', body: '{}' });
    showToast(`Cancelled reservation ${id}. Room is now empty.`);
    await refreshAll();
  } catch (err) {
    showToast(err.message, true);
  }
}

async function viewInvoice(id) {
  try {
    const res = state.reservations.find((r) => r.id === id);
    const data = await api(`/api/reservations/${encodeURIComponent(id)}/bill`);
    $('invoiceModalTitle').textContent = `Invoice — ${id}`;
    $('invoiceModalBody').innerHTML = `
      <div style="margin-bottom:12px;">
        <strong>Guest:</strong> ${escapeHtml(res?.guestName || '')}<br>
        <strong>Stay:</strong> ${escapeHtml(res?.checkInDate || '')} → ${escapeHtml(res?.checkOutDate || '')}<br>
        <strong>Status:</strong> ${escapeHtml(res?.status || '')} (${res?.paid ? 'PAID' : 'UNPAID'})
      </div>
      ${renderBillHtml(data.bill)}
    `;
    $('invoiceModal').hidden = false;
  } catch (err) {
    showToast(err.message, true);
  }
}

async function saveRecordsToFile() {
  try {
    const res = await api('/api/records/save', { method: 'POST', body: '{}' });
    const d = res.details;
    showToast(`Saved ${d.reservationCount} reservation(s) & ${d.roomCount} rooms to disk.`);
    await refreshAll();
    await loadJsonPreview();
  } catch (err) {
    showToast(err.message, true);
  }
}

async function loadJsonPreview() {
  try {
    const res = await fetch('/api/records/export/json');
    const text = await res.text();
    $('jsonPreviewBox').textContent = text;
  } catch {
    $('jsonPreviewBox').textContent = 'Unable to load preview.';
  }
}

function jumpToCalculator(roomNumber) {
  switchTab('billing');
  $('calcRoomSelect').value = String(roomNumber);
  calculateStandaloneBill();
}

function bindEvents() {
  document.querySelectorAll('.tab').forEach((btn) => {
    btn.addEventListener('click', () => switchTab(btn.dataset.tab));
  });

  $('roomStatusFilters').querySelectorAll('.chip').forEach((chip) => {
    chip.addEventListener('click', () => {
      $('roomStatusFilters').querySelectorAll('.chip').forEach((c) => c.classList.remove('active'));
      chip.classList.add('active');
      state.roomStatusFilter = chip.dataset.status;
      refreshAll();
    });
  });

  $('roomTypeFilter').addEventListener('change', (e) => {
    state.roomTypeFilter = e.target.value;
    refreshAll();
  });

  $('resStatusFilters').querySelectorAll('.chip').forEach((chip) => {
    chip.addEventListener('click', () => {
      $('resStatusFilters').querySelectorAll('.chip').forEach((c) => c.classList.remove('active'));
      chip.classList.add('active');
      state.resStatusFilter = chip.dataset.status;
      refreshAll();
    });
  });

  $('resSearchInput').addEventListener('input', (e) => {
    state.resSearch = e.target.value;
    refreshAll();
  });

  $('checkEmptyBtn').addEventListener('click', loadEmptyRooms);
  $('emptyTypeFilter').addEventListener('change', loadEmptyRooms);
  $('emptyGuestsFilter').addEventListener('change', loadEmptyRooms);

  $('newReservationBtn').addEventListener('click', () => openBookingModal());
  $('saveRecordsBtn').addEventListener('click', saveRecordsToFile);
  $('manualSaveBtn').addEventListener('click', saveRecordsToFile);

  $('closeResModalBtn').addEventListener('click', () => ($('reservationModal').hidden = true));
  $('cancelResModalBtn').addEventListener('click', () => ($('reservationModal').hidden = true));
  $('reservationForm').addEventListener('submit', submitReservationForm);

  ['resRoomSelect', 'resCheckIn', 'resCheckOut', 'resCustomFee', 'resDiscount', 'resPromoCode'].forEach((id) => {
    $(id).addEventListener('change', updateModalBillPreview);
    $(id).addEventListener('input', updateModalBillPreview);
  });

  $('billCalcForm').addEventListener('submit', (e) => {
    e.preventDefault();
    calculateStandaloneBill();
  });
  ['calcRoomSelect', 'calcNights', 'calcCustomFee', 'calcCustomFeeLabel', 'calcDiscount', 'calcPromo'].forEach((id) => {
    $(id).addEventListener('change', calculateStandaloneBill);
  });

  $('bookFromCalcBtn').addEventListener('click', () => {
    openBookingModal(Number($('calcRoomSelect').value), true);
  });

  $('printCalcInvoiceBtn').addEventListener('click', () => window.print());
  $('printModalInvoiceBtn').addEventListener('click', () => window.print());
  $('closeInvoiceModalBtn').addEventListener('click', () => ($('invoiceModal').hidden = true));
  $('closeInvoiceOkBtn').addEventListener('click', () => ($('invoiceModal').hidden = true));
}

window.openBookingModal = openBookingModal;
window.openEditModal = openEditModal;
window.doCheckIn = doCheckIn;
window.doCheckOut = doCheckOut;
window.doCancel = doCancel;
window.viewInvoice = viewInvoice;
window.jumpToCalculator = jumpToCalculator;

document.addEventListener('DOMContentLoaded', async () => {
  bindEvents();
  await refreshAll();
});
