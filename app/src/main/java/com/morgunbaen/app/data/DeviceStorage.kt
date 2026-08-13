package com.morgunbaen.app.data

import android.content.Context

/**
 * Geymsla sem er laesileg ADUR en notandinn opnar simann eftir endurraesingu.
 *
 * Android hefur tvo geymslusvaedi:
 *
 *   - Credential protected (sjalfgefid): dulkodad thar til notandinn slaer inn PIN.
 *   - Device protected: laesilegt strax vid raesingu.
 *
 * Endurraesist siminn kl. 03:00 - vegna uppfaerslu, tomrar rafhlodu i hledslu
 * eda kerfishruns - tha situr hann a laesta skjanum tangad til einhver slaer
 * inn PIN. Vaeru stillingarnar og hljodskrain a sjalfgefna svaedinu gaeti
 * appid hvorki lesid hvenaer a ad hringja ne hvad a ad spila.
 *
 * Tess vegna byr ALLT sem vekjarinn tarf her.
 */
val Context.deviceStorage: Context
    get() = createDeviceProtectedStorageContext()
