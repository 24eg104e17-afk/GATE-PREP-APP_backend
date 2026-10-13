package com.gateprep.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GATE 2027 EC syllabus (IIT Madras): subjects and their topics, in order. */
final class SyllabusData {
    private SyllabusData() {
    }

    static final Map<String, List<String>> SYLLABUS = new LinkedHashMap<>();

    static {
        SYLLABUS.put("General Aptitude", List.of(
                "Verbal: grammar, vocabulary, comprehension, narrative sequencing",
                "Quantitative: data interpretation, numerical computation and estimation, mensuration, statistics and probability",
                "Analytical: logic, deduction and induction, analogy, numerical reasoning",
                "Spatial: shape transformation, paper folding and cutting, 2D and 3D patterns"));
        SYLLABUS.put("Engineering Mathematics", List.of(
                "Linear Algebra",
                "Calculus",
                "Differential Equations",
                "Vector Analysis",
                "Complex Analysis",
                "Probability and Statistics"));
        SYLLABUS.put("Networks, Signals and Systems", List.of(
                "Circuit analysis: node and mesh, network theorems, reciprocity",
                "Sinusoidal steady state: phasors, complex power, maximum power transfer",
                "Time and frequency domain analysis: RL, RC, RLC, Laplace, 2-port, wye-delta",
                "LTI systems: causality, stability, impulse response, convolution, frequency response",
                "Continuous-time signals: Fourier series and transform, sampling",
                "Discrete-time signals: DTFT, DFT, z-transform, FIR and IIR filter design"));
        SYLLABUS.put("Electronic Devices", List.of(
                "Energy bands, intrinsic and extrinsic semiconductors, carrier concentration",
                "Carrier transport: drift, diffusion, mobility, continuity equations",
                "P-N junction, Zener diode, BJT, MOS capacitor, MOSFET, scaling",
                "LED, photodiode and solar cell"));
        SYLLABUS.put("Analog Circuits", List.of(
                "Diode circuits: clipping, clamping, rectifiers",
                "BJT and MOSFET amplifiers: biasing, small signal analysis, frequency response, current mirrors, differential amplifiers",
                "Op-amp circuits: summers, integrators, active filters, oscillators, compensation, phase margin"));
        SYLLABUS.put("Digital Circuits", List.of(
                "Number representations and combinational circuits",
                "Sequential circuits: flip-flops, counters, FSMs, setup and hold time",
                "Data converters: sample and hold, ADCs and DACs",
                "Semiconductor memories: ROM, SRAM, DRAM",
                "Computer organization: instructions, addressing modes, ALU, pipelining"));
        SYLLABUS.put("Control Systems", List.of(
                "Feedback principle, transfer function, block diagram, signal flow graph",
                "Transient and steady-state analysis, frequency response",
                "Routh-Hurwitz and Nyquist stability criteria",
                "Bode and root-locus plots",
                "Compensators and PID controller",
                "State variable model and state equation solution"));
        SYLLABUS.put("Communications", List.of(
                "Random processes: autocorrelation, power spectral density, white noise",
                "Analog communications: AM, FM, spectra, superheterodyne receivers",
                "Information theory: entropy, source coding, mutual information, channel capacity",
                "Digital communications: PCM, DPCM, ASK/PSK/FSK/QAM, ISI, ML detection, matched filter, BER",
                "Error correction: Hamming codes, CRC"));
        SYLLABUS.put("Electromagnetics", List.of(
                "Maxwell's equations and boundary conditions",
                "Plane waves: reflection, refraction, polarization, skin depth",
                "Transmission lines: impedance matching, S-parameters, Smith chart",
                "Waveguides, optical fibers, antennas and arrays"));
    }
}
