MIDIRecorder {
	var <device, <chan, <buffers, <mididefs, key_array;

	*new {
		arg chan, device;
		var buffers, key_array, mididefs;

		// initialise midi
		MIDIClient.init;

		// if device specified, connect it, otherwise connect all devices
		if (
			device != nil,
			{MIDIIn.connect(device); device = MIDIClient.sources[device]}, {MIDIIn.connectAll; device = "all connected"}
		);

		key_array = Array.newClear(128);
		buffers = Array.new;

		// disable mididefs as starting position
		mididefs.do({
			arg mididef;
			mididef.disable
		})

		^super.newCopyArgs(device, chan, buffers, mididefs, key_array)
	}

	init_mididefs {

		mididefs = [

			// noteOn standard
			MIDIdef.noteOn(\keypress, {
				arg vel, note;
				var timestamp = SystemClock.seconds;
				if (
					(key_array[note].size == 0) && (key_array[note] != \used),
					{
						key_array[note] = key_array[note].add([note, vel, timestamp]);
					},
					{
						// clear the array with a noteoff and then add note info ... do later
					}
				)

			}, chan: chan);
			// noteOff standard
			MIDIdef.noteOff(\keyup, {
				arg vel, note;
				var timestamp = SystemClock.seconds;
				if (
					(key_array[note].size == 1) && (key_array[note] != \used),
					{
						var e;
						e = MIDIRecEvent.new(note: note, vel: vel, onset: key_array[note][0][2], sus: timestamp - key_array[note][0][2]);
						// I'm sure there's a more efficient way of getting the index, so I don't need to repeatedly calculate...
						buffers[buffers.size - 1] = buffers[buffers.size - 1].add(e);
						key_array[note] = \used;

					},
					{
						// figure out what the alternative cases are ...
					}
				)
			}, chan: chan);
		];
	}

	record {
		if (
			buffers.size == 0,
			{ // construce mididefs
				this.init_mididefs; mididefs.do({arg mididef; mididef.disable})
			}
		);
		// create buffer, and enable mididefs
		buffers = buffers.add(Array.new);


		mididefs.do({
			arg mididef;
			mididef.enable
		})
	}

	stop {
		var buffer = buffers[buffers.size - 1], timestamp = SystemClock.seconds;
		mididefs.do({
			arg mididef;
			mididef.disable
		});
		// clean-up ...
		buffer.sort({arg a, b; b.onset > a.onset});
		(buffer.size - 1).do({
			arg i;
			buffer[i].delta = (buffer[i+1].onset - buffer[i].onset)
		});
		// make the last note duration the same as the sustain // or alternatively, I could take a final timestamp at the point at which the recording is stopped, and that could be the final duration ...
		buffer[buffer.size - 1].delta = timestamp - buffer[buffer.size - 1].onset;
	}

	play_buffer {

		arg buffer_index, midiout, repeats;
		var notes, vels, deltas, durs;

		notes = buffers[buffer_index].collect({
			arg e;
			e.note
		});
		vels = buffers[buffer_index].collect({
			arg e;
			e.vel
		});
		deltas = buffers[buffer_index].collect({
			arg e;
			e.delta
		});
		durs = buffers[buffer_index].collect({
			arg e;
			e.sus
		});

		notes.postln;
		vels.postln;
		deltas.postln;
		durs.postln;

		// do midiout check ...

		Pbind(
			\type, \midi,
			\midiout, midiout,
			\midicmd, \noteOn,
			\chan, 0,
			\midinote, Pseq(notes, repeats),
			// add velocity calc with \amp
			\delta, Pseq(deltas, repeats),
			\dur, Pseq(durs, repeats),
		).play
	}

	get_keyarray {
		^key_array
	}
}

// didn't work for some reason ...

// Hm I think this has something to do with the fact that the MIDIdef is defined inside the new method ... so perhaps it's referring to a different buffer???

// try defining the full MIDIdef in the record method?

// need to also add velocity responder ... both attack and release ideally ...

// so for O Dowland ... when standby is on, first note will trigger record ... or pedal triggers first record ... there needs to be a release trigger ... after that, this recording window will expand ... triggered to start and stop each time the first window reaches the end of playback ...

// I will set this to happen some number of times, during which the piano continues adding material ...

// It might be that the added material is quite straightforward -- could be that a texture is built up ... and this is used to play upon ... disintegrating all the time ... 