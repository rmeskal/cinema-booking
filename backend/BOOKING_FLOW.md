# Seat holds and booking confirmation

Booking creation requires an active Redis hold owned by the supplied user ID.
Existing screening, user, seat and room validation still runs first. A missing,
expired or incorrectly owned hold returns HTTP 409. Redis connection failures
and timeouts before commit return HTTP 503 without confirming a booking.

Hold acquisition, hold release and booking confirmation lock the same PostgreSQL
seat row. The lock lasts until the SQL transaction finishes and works across app
instances. It also serializes requests for that physical seat across screenings;
this deliberately uses the existing seat entity rather than adding a lock table.
The bookings table's unique constraint remains the final double-booking safeguard.

With the lock held, Redis `SET IFEQ` atomically replaces the owner's hold with a
unique confirmation marker and removes its expiry. The marker contains the owner
and original expiry. The seat therefore cannot expire, be reacquired or be
released through the hold endpoints while PostgreSQL is committing. A confirmation
that starts with a valid hold may finish after the original hold deadline.
These operations use Spring Data Redis's native commands, requiring Redis 8.4+;
there is no Lua.

Spring checks the marker again before SQL commit. Its transaction-completion
callback deletes that exact marker after commit, or conditionally restores the
owner and original absolute expiry after rollback. An expired deadline is not
renewed. Unique markers keep delayed callbacks from affecting later holds, even
when the same user reacquires the seat.

Post-commit Redis cleanup failure is logged and does not turn a successful booking
into a failed response. If cleanup fails, a claim response is lost, or the process
stops, the marker remains. The next hold acquisition, release or booking attempt
takes the SQL seat lock and recovers the marker from the actual database state:
delete it when booked, otherwise restore its original expiry. An unknown SQL
commit outcome is also deferred to this recovery rather than guessed.

Recovery is request-driven, so an abandoned unbooked marker can appear as HELD
until the next mutating request for that seat. There is no atomic transaction
across Redis and PostgreSQL: arbitrary Redis data loss, eviction or direct writes
outside this protocol cannot have the same guarantees as application operations.
PostgreSQL remains authoritative for confirmed bookings. Authentication is still
pending; user IDs are supplied by callers.

Run `./mvnw test` from `backend` with Docker available and the existing PostgreSQL
test database configured. `BookingFlowTests` commits its fixtures and runs outside
an enclosing test transaction so it checks real commit and rollback behavior.
