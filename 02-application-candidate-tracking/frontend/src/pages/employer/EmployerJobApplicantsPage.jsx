import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useToast } from '../../context/ToastContext';
import api from '../../services/api';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import StatusBadge from '../../components/common/StatusBadge';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Button from '../../components/common/Button';
import { 
  ArrowLeft, Users, Download, Calendar, CheckCircle, 
  XCircle, Clock, FileText, UserCheck, MessageSquare, ExternalLink, Heart
} from 'lucide-react';

export default function EmployerJobApplicantsPage() {
  const { id } = useParams();
  const { showToast } = useToast();

  const [job, setJob] = useState(null);
  const [applicants, setApplicants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('ALL');

  // Interview Schedule Modal
  const [isScheduleOpen, setIsScheduleOpen] = useState(false);
  const [scheduleTargetApp, setScheduleTargetApp] = useState(null);
  const [scheduling, setScheduling] = useState(false);
  const [interviewForm, setInterviewForm] = useState({
    scheduledAt: '',
    interviewType: 'ONLINE',
    meetingLink: '',
    location: '',
    notes: ''
  });

  useEffect(() => {
    fetchData();
  }, [id]);

  const fetchData = async () => {
    setLoading(true);
    try {
      // 1. Fetch Job
      try {
        const jRes = await api.get(`/jobs/public/${id}`);
        setJob(jRes.data);
      } catch (err) {
        console.warn('Job fetch error', err);
      }

      // 2. Fetch Applicants
      const aRes = await api.get(`/applications/job/${id}`);
      const raw = aRes.data?.content || (Array.isArray(aRes.data) ? aRes.data : []);
      setApplicants(raw);
    } catch (error) {
      showToast('Failed to load job applicants', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (appId, newStatus) => {
    try {
      await api.patch(`/applications/${appId}/status?status=${newStatus}`);
      showToast(`Applicant status changed to ${newStatus}`, 'success');
      setApplicants(prev => prev.map(a => a.id === appId ? { ...a, status: newStatus } : a));
    } catch (error) {
      showToast('Failed to update applicant status', 'error');
    }
  };

  const openInterviewModal = (app) => {
    setScheduleTargetApp(app);
    // Set default schedule date to tomorrow 10:00 AM
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(10, 0, 0, 0);
    const localIso = tomorrow.toISOString().substring(0, 16);

    setInterviewForm({
      scheduledAt: localIso,
      interviewType: 'ONLINE',
      meetingLink: 'https://meet.google.com/new',
      location: '',
      notes: 'Initial technical and cultural evaluation interview.'
    });
    setIsScheduleOpen(true);
  };

  const handleScheduleSubmit = async (e) => {
    e.preventDefault();
    if (!scheduleTargetApp) return;

    setScheduling(true);
    try {
      const payload = {
        applicationId: scheduleTargetApp.id,
        scheduledAt: `${interviewForm.scheduledAt}:00`,
        locationType: interviewForm.interviewType,
        locationOrLink: interviewForm.interviewType === 'ONLINE' ? interviewForm.meetingLink : interviewForm.location,
        instructions: interviewForm.notes
      };

      await api.post('/interviews', payload);
      showToast('Interview scheduled successfully and applicant notified!', 'success');
      // Also update status to INTERVIEW_SCHEDULED
      handleUpdateStatus(scheduleTargetApp.id, 'INTERVIEW_SCHEDULED');
      setIsScheduleOpen(false);
    } catch (error) {
      showToast(error.response?.data?.message || 'Failed to schedule interview', 'error');
    } finally {
      setScheduling(false);
    }
  };

  const handleDownloadCv = async (candidateId, candidateName) => {
    try {
      const res = await api.get(`/candidates/${candidateId}/profile/pdf`, {
        responseType: 'blob'
      });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${candidateName.replace(/\s+/g, '_')}_CV.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      showToast('CV downloaded successfully', 'success');
    } catch (error) {
      showToast('Failed to download CV', 'error');
    }
  };

  const filtered = applicants.filter(a => {
    if (filterStatus === 'ALL') return true;
    return a.status === filterStatus;
  });

  const statuses = ['ALL', 'APPLIED', 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'SELECTED', 'REJECTED'];

  if (loading) {
    return <LoadingSpinner text="Loading candidate applicants..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link to="/employer/jobs" className="btn btn-outline btn-sm p-2">
            <ArrowLeft size={16} />
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
              Applicants: {job?.title || 'Vacancy'}
            </h1>
            <p className="text-xs text-gray-500">
              {applicants.length} total applicant(s) • Closes {job?.deadline ? new Date(job.deadline).toLocaleDateString() : 'N/A'}
            </p>
          </div>
        </div>

        <Link to={`/jobs/${id}`} className="btn btn-outline btn-sm flex items-center gap-1 self-start sm:self-auto">
          Public Listing <ExternalLink size={12} />
        </Link>
      </div>

      {/* Filter Tabs */}
      <div className="flex flex-wrap gap-2 border-b border-gray-200 dark:border-gray-800 pb-3">
        {statuses.map(st => {
          const count = st === 'ALL' ? applicants.length : applicants.filter(a => a.status === st).length;
          return (
            <button
              key={st}
              onClick={() => setFilterStatus(st)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition ${
                filterStatus === st 
                  ? 'bg-primary-600 text-white shadow-sm' 
                  : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300 hover:bg-gray-200'
              }`}
            >
              {st.replace('_', ' ')} ({count})
            </button>
          );
        })}
      </div>

      {/* Applicants List */}
      {filtered.length === 0 ? (
        <div className="card p-12 text-center">
          <Users className="mx-auto text-gray-300 dark:text-gray-600 mb-3" size={40} />
          <h3 className="font-bold text-gray-800 dark:text-gray-200">No applicants in this stage</h3>
          <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
            Change the stage filter or wait for candidate submissions.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map(app => (
            <div key={app.id} className="card p-5 border border-gray-200 dark:border-gray-800 hover:shadow-md transition">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="space-y-2 flex-1">
                  <div className="flex flex-wrap items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900/60 text-primary-600 flex items-center justify-center font-bold text-sm">
                      {app.candidateName?.charAt(0) || 'C'}
                    </div>
                    <div>
                      <h3 className="font-bold text-gray-900 dark:text-white text-base">
                        {app.candidateName || 'Applicant'}
                      </h3>
                      <p className="text-xs text-gray-500">{app.candidateEmail}</p>
                    </div>
                    <StatusBadge status={app.status} />
                  </div>

                  <div className="flex flex-wrap items-center gap-3 text-xs text-gray-500 pt-1">
                    <span className="flex items-center gap-1">
                      <Clock size={12} /> Applied on {new Date(app.appliedAt).toLocaleDateString()}
                    </span>
                    {app.resumeFilename && (
                      <span className="flex items-center gap-1 text-primary-600">
                        <FileText size={12} /> Resume: {app.resumeFilename}
                      </span>
                    )}
                  </div>

                  {app.coverLetter && (
                    <div className="mt-2 p-3 bg-gray-50 dark:bg-gray-800/40 rounded-lg text-xs text-gray-600 dark:text-gray-300 border border-gray-100 dark:border-gray-800">
                      <span className="font-semibold text-gray-700 dark:text-gray-200">Candidate Note:</span> {app.coverLetter}
                    </div>
                  )}
                </div>

                {/* Candidate Action Buttons & Stage Pipeline */}
                <div className="flex flex-wrap items-center gap-2 shrink-0 border-t md:border-t-0 pt-3 md:pt-0">
                  {/* Download Generated PDF CV */}
                  <button
                    onClick={() => handleDownloadCv(app.candidateId, app.candidateName)}
                    className="btn btn-sm btn-outline flex items-center gap-1"
                    title="Download Formatted PDF Resume"
                  >
                    <Download size={14} /> PDF CV
                  </button>

                  {/* Stage Dropdown */}
                  <select
                    value={app.status}
                    onChange={(e) => handleUpdateStatus(app.id, e.target.value)}
                    className="select text-xs py-1.5 px-2.5 rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 font-medium"
                  >
                    <option value="APPLIED">Applied</option>
                    <option value="SHORTLISTED">Shortlist</option>
                    <option value="INTERVIEW_SCHEDULED">Interview Scheduled</option>
                    <option value="SELECTED">Selected / Offer</option>
                    <option value="REJECTED">Reject</option>
                  </select>

                  {/* Quick Schedule Interview Modal Trigger */}
                  <button
                    onClick={() => openInterviewModal(app)}
                    className="btn btn-sm btn-primary flex items-center gap-1 shadow"
                    title="Schedule Interview Round"
                  >
                    <Calendar size={14} /> Interview
                  </button>

                  {/* Direct Message Link */}
                  <Link
                    to={`/messages?userId=${app.candidateUserId || ''}`}
                    className="btn btn-sm btn-outline p-2"
                    title="Direct Chat"
                  >
                    <MessageSquare size={14} />
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Schedule Interview Modal */}
      <Modal
        isOpen={isScheduleOpen}
        onClose={() => setIsScheduleOpen(false)}
        title={`Schedule Interview: ${scheduleTargetApp?.candidateName || 'Applicant'}`}
      >
        <form onSubmit={handleScheduleSubmit} className="space-y-4">
          <Input
            label="Date & Time *"
            type="datetime-local"
            value={interviewForm.scheduledAt}
            onChange={(e) => setInterviewForm({ ...interviewForm, scheduledAt: e.target.value })}
            required
          />

          <Select
            label="Interview Format"
            value={interviewForm.interviewType}
            onChange={(e) => setInterviewForm({ ...interviewForm, interviewType: e.target.value })}
            options={[
              { value: 'ONLINE', label: 'Online Video Call' },
              { value: 'IN_PERSON', label: 'In-Person Onsite' }
            ]}
          />

          {interviewForm.interviewType === 'ONLINE' ? (
            <Input
              label="Meeting Link / Online URL"
              value={interviewForm.meetingLink}
              onChange={(e) => setInterviewForm({ ...interviewForm, meetingLink: e.target.value })}
              placeholder="e.g. Google Meet, Zoom, MS Teams link"
              required
            />
          ) : (
            <Input
              label="Physical Location / Office Address"
              value={interviewForm.location}
              onChange={(e) => setInterviewForm({ ...interviewForm, location: e.target.value })}
              placeholder="e.g. Level 5, LankaHire Tower, Colombo 03"
              required
            />
          )}

          <div>
            <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
              Instructions & Notes for Candidate
            </label>
            <textarea
              rows={3}
              value={interviewForm.notes}
              onChange={(e) => setInterviewForm({ ...interviewForm, notes: e.target.value })}
              className="input"
              placeholder="Provide agenda, interviewers, or preparation tips..."
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-gray-200 dark:border-gray-800">
            <button
              type="button"
              onClick={() => setIsScheduleOpen(false)}
              className="btn btn-outline"
            >
              Cancel
            </button>
            <Button
              type="submit"
              variant="primary"
              loading={scheduling}
              className="flex items-center gap-2"
            >
              <Calendar size={16} /> Send Interview Invitation
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
