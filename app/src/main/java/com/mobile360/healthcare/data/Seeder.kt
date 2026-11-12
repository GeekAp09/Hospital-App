package com.mobile360.healthcare.data

import com.mobile360.healthcare.R
import com.mobile360.healthcare.data.local.AccountEntity
import com.mobile360.healthcare.data.local.AppointmentEntity
import com.mobile360.healthcare.data.local.AvailabilityEntity
import com.mobile360.healthcare.data.local.CareDao
import com.mobile360.healthcare.data.local.DoctorEntity
import com.mobile360.healthcare.data.local.MedicationEntity
import com.mobile360.healthcare.data.local.MessageEntity
import com.mobile360.healthcare.data.local.NoteEntity
import com.mobile360.healthcare.data.local.PrescriptionEntity
import com.mobile360.healthcare.data.local.SlotEntity
import com.mobile360.healthcare.data.local.TimelineEntity
import com.mobile360.healthcare.data.model.AppointmentStatus
import com.mobile360.healthcare.data.model.MessageStatus
import com.mobile360.healthcare.data.model.TimelineType
import com.mobile360.healthcare.util.AppConfig
import com.mobile360.healthcare.util.Times
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

object Ids {
    const val MAYA = "p_maya"
    const val PRIYA = "p_priya"
    const val RAHUL = "p_rahul"
    const val ANANYA = "p_ananya"
    const val KARAN = "p_karan"
    const val MEERA_ACCOUNT = "u_meera"
    const val RAO_ACCOUNT = "u_rao"
    const val RAO = "d_rao"
    const val KHAN = "d_khan"
    const val LI_WEI = "d_liwei"
    const val MEERA = "d_meera"
    fun slot(doctorId: String, millis: Long) = "slot_${doctorId}_$millis"
}

@Singleton
class Seeder @Inject constructor(private val dao: CareDao) {
    private val mutex = Mutex()

    suspend fun ensureSeeded() = mutex.withLock {
        if (dao.doctorCount() == 0) {
            seedAll()
        } else {
            dao.insertAccounts(accounts())
        }
        dao.deletePastFreeSlots(System.currentTimeMillis())
        dao.activeDoctors().forEach { generateSlots(it.id) }
    }

    suspend fun regenerateFreeSlots(doctorId: String) = mutex.withLock {
        dao.deleteFutureFreeSlots(doctorId, System.currentTimeMillis())
        generateSlots(doctorId)
    }

    private suspend fun generateSlots(doctorId: String) {
        val avail = dao.availabilityOnce(doctorId)
        val today = LocalDate.now()
        val slots = mutableListOf<SlotEntity>()
        for (day in 1..14) {
            val date = today.plusDays(day.toLong())
            val dow = date.dayOfWeek.value
            val windows: List<Pair<LocalTime, LocalTime>> = if (avail.isEmpty()) {
                if (dow <= 5) listOf(LocalTime.of(10, 30) to LocalTime.of(12, 30), LocalTime.of(14, 0) to LocalTime.of(15, 0))
                else emptyList()
            } else {
                avail.firstOrNull { it.dayOfWeek == dow && it.enabled }
                    ?.let { listOf(LocalTime.parse(it.startTime) to LocalTime.parse(it.endTime)) }
                    ?: emptyList()
            }
            for ((start, end) in windows) {
                var t = start
                while (t.isBefore(end)) {
                    val millis = Times.startOfDay(date) + (t.toSecondOfDay() * 1000L)
                    slots += SlotEntity(Ids.slot(doctorId, millis), doctorId, millis, false)
                    t = t.plusMinutes(30)
                }
            }
        }
        dao.insertSlots(slots)
    }

    private suspend fun seedAll() {
        val tuesday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
        val tueOffset = (tuesday.toEpochDay() - LocalDate.now().toEpochDay()).toInt()

        dao.insertDoctors(doctors())
        dao.insertAccounts(accounts())
        dao.insertAvailability(meeraAvailability())
        dao.insertAvailability(raoAvailability())
        dao.activeDoctors().forEach { generateSlots(it.id) }

        val raoTue1030 = Times.at(tueOffset, 10, 30)
        dao.setSlotBooked(Ids.slot(Ids.RAO, raoTue1030), true)
        dao.setSlotBooked(Ids.slot(Ids.RAO, Times.at(tueOffset, 11, 0)), true)

        dao.insertAppointments(
            listOf(
                AppointmentEntity(
                    id = "a_rao_next", patientId = Ids.MAYA, doctorId = Ids.RAO, slotId = Ids.slot(Ids.RAO, raoTue1030),
                    startMillis = raoTue1030, status = AppointmentStatus.CONFIRMED, reason = "Cardiology follow-up",
                    room = "Building B, Suite 402", instructions = "Arrive 15 mins early", prepNote = "Bring fasting blood panel results"
                ),
                AppointmentEntity(
                    id = "a_rao_past", patientId = Ids.MAYA, doctorId = Ids.RAO, slotId = null,
                    startMillis = Times.at(-5, 10, 30), status = AppointmentStatus.COMPLETED, reason = "Blood pressure review",
                    room = "Building B, Suite 402"
                ),
                AppointmentEntity(
                    id = "a_khan_past", patientId = Ids.MAYA, doctorId = Ids.KHAN, slotId = null,
                    startMillis = Times.at(-18, 11, 0), status = AppointmentStatus.COMPLETED, reason = "Stomach discomfort",
                    room = "Building A, Suite 210"
                ),
                AppointmentEntity(
                    id = "a_liwei_cancelled", patientId = Ids.MAYA, doctorId = Ids.LI_WEI, slotId = null,
                    startMillis = Times.at(-12, 9, 0), status = AppointmentStatus.CANCELLED, reason = "Breathing check",
                    room = "Building C, Suite 118", instructions = "Follow-up recommended", cancelNote = "Cancelled by patient"
                ),
                AppointmentEntity(
                    id = "a_priya", patientId = Ids.PRIYA, doctorId = Ids.MEERA, slotId = null,
                    startMillis = Times.at(0, 9, 0), status = AppointmentStatus.CONFIRMED, reason = "General Cardiology Follow-up",
                    room = "Room 302", fileNo = "CR-4412", vitals = "BP 120/78 mmHg"
                ),
                AppointmentEntity(
                    id = "a_priya_past", patientId = Ids.PRIYA, doctorId = Ids.KHAN, slotId = null,
                    startMillis = Times.at(-25, 10, 0), status = AppointmentStatus.COMPLETED, reason = "Pre-procedure Health Evaluation",
                    room = "Building A, Suite 210", instructions = "Maintain current supplements"
                ),
                AppointmentEntity(
                    id = "a_rahul", patientId = Ids.RAHUL, doctorId = Ids.MEERA, slotId = null,
                    startMillis = Times.at(0, 9, 30), status = AppointmentStatus.PENDING, reason = "ECG Review & Chest Tightness",
                    room = "Room 302", fileNo = "CR-4420", referral = "Referred by Dr. S. Kulkarni • Urgent review requested"
                ),
                AppointmentEntity(
                    id = "a_rahul_past", patientId = Ids.RAHUL, doctorId = "d_verma", slotId = null,
                    startMillis = Times.at(-35, 11, 30), status = AppointmentStatus.COMPLETED, reason = "Skin Allergy & Reaction Check",
                    room = "Building A, Suite 105", instructions = "Apply prescribed topical cream"
                ),
                AppointmentEntity(
                    id = "a_ananya", patientId = Ids.ANANYA, doctorId = Ids.MEERA, slotId = null,
                    startMillis = Times.at(0, 11, 15), status = AppointmentStatus.CONFIRMED, reason = "Post-Angioplasty Check",
                    room = "Room 302", fileNo = "CR-4398"
                ),
                AppointmentEntity(
                    id = "a_karan", patientId = Ids.KARAN, doctorId = Ids.MEERA, slotId = null,
                    startMillis = Times.at(0, 12, 0), status = AppointmentStatus.CONFIRMED, reason = "Hypertension Follow-up",
                    room = "Room 302", fileNo = "CR-4405"
                ),
                AppointmentEntity(
                    id = "a_rao_karan", patientId = Ids.KARAN, doctorId = Ids.RAO, slotId = null,
                    startMillis = Times.at(0, 14, 0), status = AppointmentStatus.CONFIRMED, reason = "Lipid & Hypertension Protocol",
                    room = "Building B, Suite 402", fileNo = "CR-4405", vitals = "BP 138/86 mmHg"
                ),
                AppointmentEntity(
                    id = "a_rao_pending", patientId = Ids.ANANYA, doctorId = Ids.RAO, slotId = null,
                    startMillis = Times.at(0, 15, 30), status = AppointmentStatus.PENDING, reason = "Second opinion on post-angioplasty medication",
                    room = "Building B, Suite 402", referral = "Referred by Dr. Iyer for senior opinion"
                )
            )
        )

        val rxDesc = "Maintain low-sodium diet and hydrate well. Schedule repeat lipid profile in 4 weeks."
        dao.insertPrescriptions(
            listOf(
                PrescriptionEntity("rx_rao", "CR-8821", Ids.MAYA, Ids.RAO, "a_rao_past", Times.at(-5, 11, 0), "ACTIVE", rxDesc, 1, Times.at(-5, 11, 5)),
                PrescriptionEntity("rx_khan", "CR-7342", Ids.MAYA, Ids.KHAN, "a_khan_past", Times.at(-18, 11, 0), "COMPLETED", "Take after meals and finish the course.", 1, Times.at(-18, 11, 10)),
                PrescriptionEntity("rx_priya", "CR-7710", Ids.PRIYA, Ids.MEERA, "a_priya", Times.at(-30, 9, 0), "ACTIVE", rxDesc, 1, Times.at(-30, 9, 10)),
                PrescriptionEntity("rx_priya_past", "CR-5540", Ids.PRIYA, Ids.KHAN, "a_priya_past", Times.at(-25, 10, 30), "COMPLETED", "Finished 14-day anti-inflammatory regimen.", 1, Times.at(-25, 10, 35)),
                PrescriptionEntity("rx_rahul", "CR-6120", Ids.RAHUL, Ids.RAO, "a_rahul_past", Times.at(-14, 10, 0), "ACTIVE", "Take beta-blocker with morning meal. Keep sublingual nitroglycerin accessible for emergency.", 1, Times.at(-14, 10, 5))
            )
        )
        dao.insertMedications(
            listOf(
                MedicationEntity("med_rao_1", "rx_rao", "Atorvastatin 10 mg", "Lipid", "1 tablet", "Once daily (Evening after dinner)", 30, 2, "With water • Bedtime"),
                MedicationEntity("med_rao_2", "rx_rao", "Aspirin 75 mg", "Antiplatelet", "1 tablet", "After lunch", 30, 1, "Take after food"),
                MedicationEntity("med_khan_1", "rx_khan", "Pantoprazole 40 mg", "Antacid", "1 tablet", "Once daily before breakfast", 14, 0, "Take 30 minutes before food"),
                MedicationEntity("med_priya_1", "rx_priya", "Atorvastatin 10 mg", "Lipid", "1 tablet", "Daily at bedtime", 30, 2, "With water • Bedtime"),
                MedicationEntity("med_priya_2", "rx_priya", "Aspirin 75 mg", "Antiplatelet", "1 tablet", "Daily after meals", 30, 1, "Take after food"),
                MedicationEntity("med_priya_3", "rx_priya", "Metoprolol 25 mg", "Beta-Blocker", "1 tablet", "Daily with morning meal", 30, 2, "Take with food"),
                MedicationEntity("med_priya_past_1", "rx_priya_past", "Amoxicillin 500 mg", "Antibiotic", "1 capsule", "Three times daily with meals", 7, 0, "Complete full 7-day course"),
                MedicationEntity("med_rahul_1", "rx_rahul", "Metoprolol Succinate 25 mg", "Beta-Blocker", "1 tablet", "Once daily (Morning)", 30, 2, "Take with food • Monitor heart rate"),
                MedicationEntity("med_rahul_2", "rx_rahul", "Nitroglycerin 0.4 mg SL", "Vasodilator", "1 tablet", "As needed for chest pain", 25, 1, "Sublingual • Seek emergency care if unresolved after 5 mins")
            )
        )
        dao.insertTimeline(
            listOf(
                TimelineEntity("tl_rx_rao", Ids.MAYA, TimelineType.PRESCRIPTION, "Prescription issued by Dr. Arun Rao", "Atorvastatin 10 mg • Aspirin 75 mg", "", Times.at(-5, 11, 5), "rx_rao"),
                TimelineEntity("tl_note_rao", Ids.MAYA, TimelineType.CONSULTATION, "Clinical Consultation Note by Dr. Arun Rao", "Patient reports steady cardiovascular stability. BP monitored at 122/78 mmHg. Follow-up blood test ordered.", "", Times.at(-5, 10, 30), "a_rao_past"),
                TimelineEntity("tl_lab", Ids.MAYA, TimelineType.LABORATORY, "Lipid Panel & CBC", "Verified by Central Labs", "New report", Times.at(-8, 9, 0), null),
                TimelineEntity("tl_rx_khan", Ids.MAYA, TimelineType.PRESCRIPTION, "Prescription issued by Dr. Sara Khan", "Pantoprazole 40 mg", "", Times.at(-18, 11, 10), "rx_khan"),

                TimelineEntity("tl_rx_priya", Ids.PRIYA, TimelineType.PRESCRIPTION, "Prescription issued by Dr. Meera Iyer", "Atorvastatin 10 mg • Aspirin 75 mg • Metoprolol 25 mg", "", Times.at(-30, 9, 10), "rx_priya"),
                TimelineEntity("tl_note_priya", Ids.PRIYA, TimelineType.CONSULTATION, "Cardiology Consultation Note by Dr. Meera Iyer", "Patient reports improved stamina with minor morning fatigue. Blood pressure normalized at 120/78 mmHg.", "", Times.at(0, 8, 55), "a_priya"),
                TimelineEntity("tl_lab_priya", Ids.PRIYA, TimelineType.LABORATORY, "Echocardiogram & Cardiac Doppler", "Verified by Central Labs • Normal LVEF (62%), mild aortic sclerosis", "Echo Report", Times.at(-15, 11, 0), null),
                TimelineEntity("tl_appt_priya", Ids.PRIYA, TimelineType.APPOINTMENT, "Pre-procedure Health Evaluation with Dr. Sara Khan", "Completed in Building A, Suite 210", "", Times.at(-25, 10, 0), "a_priya_past"),

                TimelineEntity("tl_rahul_ref", Ids.RAHUL, TimelineType.CONSULTATION, "Urgent Cardiology Referral by Dr. S. Kulkarni", "Resting ECG showed transient ST-T fluctuations. Requested prompt specialist cardiology evaluation.", "Urgent Referral", Times.at(-1, 16, 0), "a_rahul"),
                TimelineEntity("tl_rx_rahul", Ids.RAHUL, TimelineType.PRESCRIPTION, "Prescription issued by Dr. Arun Rao", "Metoprolol Succinate 25 mg • Nitroglycerin 0.4 mg SL", "", Times.at(-14, 10, 5), "rx_rahul"),
                TimelineEntity("tl_rahul_lab", Ids.RAHUL, TimelineType.LABORATORY, "12-Lead Electrocardiogram & Cardiac Biomarkers", "Normal sinus rhythm at 68 bpm. Troponin I negative (<0.01 ng/mL).", "Verified by Central Diagnostics", Times.at(-10, 14, 0), null),
                TimelineEntity("tl_rahul_derm", Ids.RAHUL, TimelineType.APPOINTMENT, "Dermatology Review with Dr. Neha Verma", "Contact dermatitis resolved. Completed visit in Suite 105.", "", Times.at(-35, 11, 30), "a_rahul_past")
            )
        )
        dao.upsertNote(NoteEntity("a_priya", "Patient reports improved stamina with minor morning fatigue. Blood pressure normalized at 120/78 mmHg. Recommend continuing current cardiac regimen.", Times.at(0, 8, 55)))
        dao.upsertNote(NoteEntity("a_rao_karan", "BP 138/86 mmHg. Adjusted ACE inhibitor dose. Repeat renal function panel and lipid profile in 6 weeks.", Times.at(0, 8, 30)))
        dao.upsertNote(NoteEntity("a_rahul", "Patient presented with intermittent exertional chest tightness. ECG reviewed, stable hemodynamics. Scheduled for comprehensive stress test.", Times.at(0, 9, 20)))

        val now = System.currentTimeMillis()
        val min = 60_000L
        dao.insertMessages(
            listOf(
                MessageEntity("m1", "a_rao_next", Ids.RAO, "Good morning Maya. How are you feeling after adjusting the beta blocker dosage?", now - 180 * min, MessageStatus.READ),
                MessageEntity("m2", "a_rao_next", Ids.MAYA, "Much better, thanks! The morning dizziness has completely stopped.", now - 150 * min, MessageStatus.READ),
                MessageEntity("m3", "a_rao_next", Ids.RAO, "Glad to hear that. Please continue recording your BP twice daily until Tuesday.", now - 146 * min, MessageStatus.READ),
                MessageEntity("m4", "a_rao_next", Ids.MAYA, "Will do, logged 120/78 this morning.", now - 144 * min, MessageStatus.READ),
                MessageEntity("m5", "a_rao_next", Ids.MAYA, "Also, should I take the potassium supplement with breakfast or before sleep?", now - 140 * min, MessageStatus.DELIVERED),
                MessageEntity("m6", "a_khan_past", Ids.MAYA, "Thank you doctor, the discomfort is gone.", now - 3 * 1440 * min - 30 * min, MessageStatus.READ),
                MessageEntity("m7", "a_khan_past", Ids.KHAN, "Take after meals and finish the course.", now - 3 * 1440 * min, MessageStatus.READ),
                MessageEntity("m8", "a_priya", Ids.PRIYA, "Good morning doctor, my morning fatigue is a little better.", now - 200 * min, MessageStatus.READ),
                MessageEntity("m9", "a_priya", Ids.MEERA, "Good to hear, Priya. We will review it at your visit.", now - 190 * min, MessageStatus.DELIVERED),
                MessageEntity("m10", "a_rahul", Ids.RAHUL, "Good morning Dr. Meera, Dr. Kulkarni asked me to see you urgently regarding recurrent chest tightness after walking upstairs.", now - 120 * min, MessageStatus.READ),
                MessageEntity("m11", "a_rahul", Ids.MEERA, "Hello Rahul, I have received your referral notes and ECG from Dr. Kulkarni. Please take a seat in waiting Room 302, we will review your case this morning.", now - 110 * min, MessageStatus.DELIVERED),
                MessageEntity("m12", "a_priya", Ids.MEERA, "Priya, remember to bring your recent home blood pressure log when you come in today.", now - 80 * min, MessageStatus.READ),
                MessageEntity("m13", "a_priya", Ids.PRIYA, "Yes doctor, I have logged the morning and evening readings in my notebook.", now - 75 * min, MessageStatus.DELIVERED),
                MessageEntity("m14", "a_rao_karan", Ids.RAO, "Karan, please arrive 10 minutes early so the triage nurse can check your sitting and standing blood pressure.", now - 60 * min, MessageStatus.READ),
                MessageEntity("m15", "a_rao_karan", Ids.KARAN, "Sure Dr. Rao, I will be there by 1:50 PM with my medication bottles.", now - 50 * min, MessageStatus.DELIVERED),
                MessageEntity("m16", "a_rao_pending", Ids.ANANYA, "Dr. Rao, Dr. Iyer suggested I seek your opinion regarding my medication plan after the stenting.", now - 40 * min, MessageStatus.READ),
                MessageEntity("m17", "a_rao_pending", Ids.RAO, "Hello Ananya, I am reviewing your angiography summary now. We will confirm your afternoon slot shortly.", now - 35 * min, MessageStatus.DELIVERED)
            )
        )
    }

    private fun doctors() = listOf(
        DoctorEntity(Ids.RAO, "Dr. Arun Rao", "Cardiology", "Cardiology Specialist", 20, 4.8, 124, "English, Hindi", "CareConnect Lead", "CareConnect Hospital", "Building B, Suite 402 • Main Campus", "+911800120411", R.drawable.a_headshot_portrait_of_dr_arun_rao_an_ex, true, 1200, "Consults Monday to Friday."),
        DoctorEntity(Ids.KHAN, "Dr. Sara Khan", "Internal Medicine", "Internal Medicine Specialist", 14, 4.6, 98, "English, Urdu", "Care team physician", "CareConnect Hospital", "Building A, Suite 210 • Main Campus", "+911800120412", R.drawable.professional_portrait_of_dr_sara_khan_an, true, 900, "Consults Monday to Friday."),
        DoctorEntity(Ids.LI_WEI, "Dr. Li Wei", "Pulmonology", "Pulmonology Clinic", 18, 4.7, 110, "English, Mandarin", "Pulmonology lead", "CareConnect Hospital", "Building C, Suite 118 • Main Campus", "+911800120413", R.drawable.a_high_quality_professional_portrait_of, true, 1000, "Consults Monday to Friday."),
        DoctorEntity(Ids.MEERA, "Dr. Meera Iyer", "Cardiology", "Cardiology Specialist", 16, 4.9, 87, "English, Tamil", "Heart Center", "CareConnect Hospital", "Building B, Suite 305 • Main Campus", "+911800120414", R.drawable.close_up_portrait_photograph_of_a_welcom, true, 1100, "Consults Monday, Tuesday, Thursday and Friday."),
        DoctorEntity("d_verma", "Dr. Neha Verma", "Dermatology", "Dermatology Specialist", 11, 4.5, 76, "English, Hindi", "Skin clinic", "CareConnect Hospital", "Building A, Suite 105 • Main Campus", "+911800120415", R.drawable.a_confident_female_cardiologist_of_middl, true, 800, "Consults Monday to Friday."),
        DoctorEntity("d_patel", "Dr. Rohan Patel", "Pediatrics", "Pediatrics Specialist", 9, 4.7, 102, "English, Gujarati", "Children's care", "CareConnect Hospital", "Building D, Suite 020 • Main Campus", "+911800120416", R.drawable.a_clean_professional_close_up_portrait_o, true, 700, "Consults Monday to Friday."),
        DoctorEntity("d_qureshi", "Dr. Imran Qureshi", "Orthopedics", "Orthopedics Specialist", 15, 4.4, 64, "English, Urdu", "Bone and joint", "CareConnect Hospital", "Building D, Suite 140 • Main Campus", "+911800120417", R.drawable.a_distinguished_south_asian_male_doctor, true, 1000, "Consults Monday to Friday."),
        DoctorEntity("d_chen", "Dr. Daniel Chen", "Orthopedics", "Orthopedics Specialist", 12, 4.5, 71, "English, Mandarin", "Sports injuries", "CareConnect Hospital", "Building D, Suite 142 • Main Campus", "+911800120419", R.drawable.an_east_asian_male_physician_in_his_40s, true, 950, "Consults Monday to Friday."),
        DoctorEntity("d_sethi", "Dr. Vikram Sethi", "Cardiology", "Cardiology Specialist", 22, 4.6, 58, "English, Hindi", "On leave", "CareConnect Hospital", "Building B, Suite 410 • Main Campus", "+911800120418", R.drawable.professional_clinical_headshot_of_an_exp, false, 1200, "Currently on leave.")
    )

    private fun accounts() = listOf(
        AccountEntity(Ids.MAYA, AppConfig.DEMO_PATIENT_PHONE, "Maya", "PATIENT", 31, "Female", R.drawable.a_warm_professional_portrait_photograph, null),
        AccountEntity(Ids.PRIYA, AppConfig.DEMO_PRIYA_PHONE, "Priya Nair", "PATIENT", 34, "Female", R.drawable.portrait_photograph_of_priya_nair_an_ind, null),
        AccountEntity(Ids.RAHUL, AppConfig.DEMO_RAHUL_PHONE, "Rahul Mehta", "PATIENT", 48, "Male", R.drawable.portrait_photography_of_rahul_mehta_an_a, null),
        AccountEntity(Ids.ANANYA, AppConfig.DEMO_ANANYA_PHONE, "Ananya Sen", "PATIENT", 41, "Female", R.drawable.a_confident_female_cardiologist_of_middl, null),
        AccountEntity(Ids.KARAN, AppConfig.DEMO_KARAN_PHONE, "Karan Shah", "PATIENT", 52, "Male", R.drawable.small_thumbnail_avatar_of_indian_male_ca, null),
        AccountEntity(Ids.MEERA_ACCOUNT, AppConfig.DEMO_DOCTOR_PHONE, "Dr. Meera Iyer", "DOCTOR", 45, "Female", R.drawable.close_up_portrait_photograph_of_a_welcom, Ids.MEERA),
        AccountEntity(Ids.RAO_ACCOUNT, AppConfig.DEMO_DOCTOR_RAO_PHONE, "Dr. Arun Rao", "DOCTOR", 52, "Male", R.drawable.a_headshot_portrait_of_dr_arun_rao_an_ex, Ids.RAO)
    )

    private fun meeraAvailability(): List<AvailabilityEntity> {
        fun row(dow: Int, enabled: Boolean, start: String, end: String) =
            AvailabilityEntity("${Ids.MEERA}_$dow", Ids.MEERA, dow, enabled, start, end)
        return listOf(
            row(1, true, "09:00", "13:00"),
            row(2, true, "14:00", "18:00"),
            row(3, false, "09:00", "13:00"),
            row(4, true, "09:00", "13:00"),
            row(5, true, "09:00", "13:00"),
            row(6, false, "09:00", "13:00"),
            row(7, false, "09:00", "13:00")
        )
    }

    private fun raoAvailability(): List<AvailabilityEntity> {
        fun row(dow: Int, enabled: Boolean, start: String, end: String) =
            AvailabilityEntity("${Ids.RAO}_$dow", Ids.RAO, dow, enabled, start, end)
        return listOf(
            row(1, true, "10:00", "15:00"),
            row(2, true, "10:00", "15:00"),
            row(3, true, "10:00", "15:00"),
            row(4, true, "10:00", "15:00"),
            row(5, true, "10:00", "14:00"),
            row(6, false, "10:00", "14:00"),
            row(7, false, "10:00", "14:00")
        )
    }
}
