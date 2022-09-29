// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin Desktop.
//

package cds.astro;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cds.astro.Coo.distance;
import static cds.astro.Astropos.varianceToErrorEllipse;
import static cds.astro.Astropos.errorEllipseToVariance;

public class TestGaia {

	public static void main(final String[] args) throws IOException {
		final Path path = FileSystems.getDefault().getPath("data", "sample1000-gaia_source-final.csv");
		final List<String> iLines = Files.readAllLines(path);
		iLines.remove(0); // remove the header line
		final List<String> oLines = new ArrayList<String>(iLines.size());
		// 01 source_id,
		// 02 solution_id,
		// 03 designation,
		// 04 random_index,
		// 05 ref_epoch,
		// 06 ra,
		// 07 ra_error,
		// 08 dec,
		// 09 dec_error,
		// 10 parallax,
		// 11 parallax_error,
		// 12 parallax_over_error,
		// 13 pm,
		// 14 pmra,
		// 15 pmra_error,
		// 16 pmdec,
		// 17 pmdec_error,
		// 18 ra_dec_corr,
		// 19 ra_parallax_corr,
		// 20 ra_pmra_corr,
		// 21 ra_pmdec_corr,
		// 22 dec_parallax_corr,
		// 23 dec_pmra_corr,
		// 24 dec_pmdec_corr,
		// 25 parallax_pmra_corr,
		// 26 parallax_pmdec_corr,
		// 27 pmra_pmdec_corr,
		// 28 astrometric_n_obs_al,
		// 29 astrometric_n_obs_ac,
		// 30 astrometric_n_good_obs_al,
		//    astrometric_n_bad_obs_al,
		//    astrometric_gof_al,
		//    astrometric_chi2_al,
		//    astrometric_excess_noise,
		//    astrometric_excess_noise_sig,
		//    astrometric_params_solved,
		//    astrometric_primary_flag,
		//    nu_eff_used_in_astrometry,
		//    pseudocolour,
		// 40 pseudocolour_error,
		//    ra_pseudocolour_corr,
		//    dec_pseudocolour_corr,
		//    parallax_pseudocolour_corr,
		//    pmra_pseudocolour_corr,
		//    pmdec_pseudocolour_corr,
		//    astrometric_matched_transits,
		//    visibility_periods_used,
		//    astrometric_sigma5d_max,
		//    matched_transits,
		// 50 new_matched_transits,
		//    matched_transits_removed,
		//    ipd_gof_harmonic_amplitude,
		//    ipd_gof_harmonic_phase,
		//    ipd_frac_multi_peak,
		//    ipd_frac_odd_win,
		//    ruwe,
		//    scan_direction_strength_k1,
		//    scan_direction_strength_k2,
		//    scan_direction_strength_k3,
		// 60 scan_direction_strength_k4,
		//    scan_direction_mean_k1,
		//    scan_direction_mean_k2,
		//    scan_direction_mean_k3,
		//    scan_direction_mean_k4,
		//    duplicated_source,
		//    phot_g_n_obs,
		//    phot_g_mean_flux,
		//    phot_g_mean_flux_error,
		//    phot_g_mean_flux_over_error,
		// 70 phot_g_mean_mag,
		//    phot_bp_n_obs,
		//    phot_bp_mean_flux,
		//    phot_bp_mean_flux_error,
		//    phot_bp_mean_flux_over_error,
		//    phot_bp_mean_mag,
		//    phot_rp_n_obs,
		//    phot_rp_mean_flux,
		//    phot_rp_mean_flux_error,
		//    phot_rp_mean_flux_over_error,
		// 80 phot_rp_mean_mag,
		//    phot_bp_n_contaminated_transits,
		//    phot_bp_n_blended_transits,
		//    phot_rp_n_contaminated_transits,
		//    phot_rp_n_blended_transits,
		//    phot_proc_mode,
		//    phot_bp_rp_excess_factor,
		//    bp_rp,
		//    bp_g,
		//    g_rp,
		// 90 dr2_radial_velocity,
		//    dr2_radial_velocity_error,
		//    dr2_rv_nb_transits,
		//    dr2_rv_template_teff,
		//    dr2_rv_template_logg,
		//    dr2_rv_template_fe_h,
		//    l,
		//    b,
		//    ecl_lon,
		//    ecl_lat,
		// 100 allwise,
		//     panstarrs1,
		//     sdssdr13,
		//     skymapper2,
		//     urat1,
		//     phot_g_mean_mag_error,
		//     phot_bp_mean_mag_error,
		//     phot_rp_mean_mag_error,
		//     phot_g_mean_mag_corrected,
		//     phot_g_mean_mag_error_corrected,
		// 110 phot_g_mean_flux_corrected,
		//     phot_bp_rp_excess_factor_corrected,
		// 112 ra_epoch2000,
		// 113 dec_epoch2000,
		// 114 ra_epoch2000_error,
		// 115 dec_epoch2000_error,
		// 116 ra_dec_epoch2000_corr
		int i = 0;
		double max_d = 0.0;
		double max_d1 = 0.0;
		double max_d2 = 0.0;
		double max_d3 = 0.0;
		for(final String line : iLines) {
			final String[] splitted = line.split(",");
			final double ra = Double.parseDouble(splitted[5]);
			final double dec = Double.parseDouble(splitted[7]);
			final double[] eepos = new double[3];
			final double[] eepost = new double[] {
			    Double.parseDouble(splitted[6]),
			    Double.parseDouble(splitted[8]),
			    Double.parseDouble(splitted[17]) // RA:Dec
			};
			// Transforms sig/sig/rho in sig2/sig2/rhosigsig!
			eepost[2] *= eepost[0] * eepost[1];
			eepost[0] *= eepost[0];
			eepost[1] *= eepost[1];
			// System.out.println("Before: " + Arrays.toString(eepos)+ " / " + Arrays.toString(eepost));
			varianceToErrorEllipse(eepost, eepos);
			//System.out.println("After: " + Arrays.toString(eepos));

			final double mu1 = splitted[13].isEmpty() ? 0.0 : Double.parseDouble(splitted[13]);
			final double mu2 = splitted[15].isEmpty() ? 0.0 : Double.parseDouble(splitted[15]);
			final double[] eepm = new double[3];
			final double[] eepmt = new double[] {
				splitted[14].isEmpty() ? 0.0 : Double.parseDouble(splitted[14]),
				splitted[16].isEmpty() ? 0.0 : Double.parseDouble(splitted[16]),
				splitted[26].isEmpty() ? 0.0 : Double.parseDouble(splitted[26]) // pmRA:pmDec
			};
			// Transforms sig/sig/rho in sig2/sig2/rhosigsig!
			eepmt[2] *= eepmt[0] * eepmt[1];
			eepmt[0] *= eepmt[0];
			eepmt[1] *= eepmt[1];
			varianceToErrorEllipse(eepmt, eepm);
			final double[] plx2 = new double[] {
				splitted[9].isEmpty() ? 0.0 : Double.parseDouble(splitted[9]),
				splitted[10].isEmpty() ? 0.0 : Double.parseDouble(splitted[10])
			};
			final double[] rv2 = new double[] {
				splitted[89].isEmpty() ? 0.0 : Double.parseDouble(splitted[89]),
				splitted[90].isEmpty() ? 0.0 : Double.parseDouble(splitted[90])
			};
			final double[] corr = new double[] {
				splitted[17].isEmpty() ? 0.0 : Double.parseDouble(splitted[17]), // RA:Dec
				splitted[18].isEmpty() ? 0.0 : Double.parseDouble(splitted[18]), // RA:Plx
				splitted[19].isEmpty() ? 0.0 : Double.parseDouble(splitted[19]), // RA:pmRA
				splitted[20].isEmpty() ? 0.0 : Double.parseDouble(splitted[20]), // RA:pmDec
				splitted[21].isEmpty() ? 0.0 : Double.parseDouble(splitted[21]), // Dec:Plx
				splitted[22].isEmpty() ? 0.0 : 	Double.parseDouble(splitted[22]), // Dec:pmRA
				splitted[23].isEmpty() ? 0.0 : 	Double.parseDouble(splitted[23]), // Dec:pmDec
				splitted[24].isEmpty() ? 0.0 : 	Double.parseDouble(splitted[24]), // Plx:pmRA
				splitted[25].isEmpty() ? 0.0 : 	Double.parseDouble(splitted[25]), // Plx:pmDec
				splitted[26].isEmpty() ? 0.0 : 	Double.parseDouble(splitted[26])  // pmRA:pmDec
			};
			/*System.out.println("ra:" + ra + "; dec: " + dec + "; eepos: " + Arrays.toString(eepos) + "; eepost: " + Arrays.toString(eepost)
				+ "; mu1: " + mu1 + "; mu2: " + mu2 + "; eepm: " + Arrays.toString(eepm) + "; eepmt: " + Arrays.toString(eepmt)
				+ "; plx: " + Arrays.toString(plx2)
				+ "; rv: " + Arrays.toString(rv2)
				+ "; corr: " + Arrays.toString(corr) 
			);*/
			final Astropos pos = new Astropos(ICRS.create(2016.0),
					ra, dec, 2016.0, eepos, 
					mu1, mu2, 2016.0, eepm, 
					plx2, rv2);	
			if (pos.setCorrelations(corr)) {
				continue;
			} else {
				System.out.println("Set correlation failed!");
				/*System.out.println("ra:" + ra + "; dec: " + dec + "; eepos: " + Arrays.toString(eepos) + "; eepost: " + Arrays.toString(eepost)
				+ "; mu1: " + mu1 + "; mu2: " + mu2 + "; eepm: " + Arrays.toString(eepm) + "; eepmt: " + Arrays.toString(eepmt)
				+ "; plx: " + Arrays.toString(plx2)
				+ "; rv: " + Arrays.toString(rv2)
				+ "; corr: " + Arrays.toString(corr) 
						);*/
			}
			// Compute at epoch 2000s
			if (pos.toEpoch(2000.0)) {
				// Input
				final double ra_epoch2000 = Double.parseDouble(splitted[111]);
				final double de_epoch2000 = Double.parseDouble(splitted[112]);
				final double ra_epoch2000_error = Double.parseDouble(splitted[113]);
				final double de_epoch2000_error = Double.parseDouble(splitted[114]);
				final double ra_de_epoch2000_corr = Double.parseDouble(splitted[115]);
				// Output
				final double out_ra = pos.getLon();
				final double out_dec = pos.getLat();
				final double[] out_ee3 = new double[3];
				final double[] out_vvc = new double[3];
				pos.copyErrorEllipse(out_ee3);
				errorEllipseToVariance(out_ee3, out_vvc);
				
				final double d = distance(ra_epoch2000, de_epoch2000, out_ra, out_dec) / 3_600_000.0;
				final double d1 = Math.abs(ra_epoch2000_error - Math.sqrt(out_vvc[0]));
				final double d2 = Math.abs(de_epoch2000_error - Math.sqrt(out_vvc[1]));
				final double d3 = Math.abs(ra_de_epoch2000_corr - out_vvc[2]/Math.sqrt(out_vvc[0] * out_vvc[1]));
				if (max_d < d) {
					max_d = d;
				}
				if (max_d1 < d1) {
					max_d1 = d1;
				}
				if (max_d2 < d2) {
					max_d2 = d2;
				}
				if (max_d3 < d3) {
					max_d3 = d3;
				}
				
				//System.out.println(line);
				System.out.println("i: " + i + "; d: " + d + " mas, " 
					//+ Arrays.toString(out_vvc)
					+ " diff1: " + d1 
					+ " diff2: " + d2
					+ " diff3: " + d3
				);
				// take j2000 and compute differences
			} else {
				System.out.println("Conversion to J2000.0 failed!");
			}
			i++;
		} 
		System.out.println("d: " + max_d + "; d1: " + max_d1 + "; d2: " + max_d2 + "; d3: " + max_d3); 
	}
	
}
