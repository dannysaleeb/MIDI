MIDIRecEvent {
	var <>note, <>vel, <>onset, <>sus, <>delta;

	*new {
		arg note, vel, onset, sus, delta;

		^super.newCopyArgs(note, vel, onset, sus, delta)
	}
}