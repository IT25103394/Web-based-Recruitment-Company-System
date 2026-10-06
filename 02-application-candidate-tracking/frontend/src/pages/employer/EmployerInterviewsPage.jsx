import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useToast } from '../../context/ToastContext';
import api from '../../services/api';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import StatusBadge from '../../components/common/StatusBadge';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Button from '../../components/common/Button';
import { Calendar, Video, Clock, User, ExternalLink, CheckCircle, XCircle, Edit2, Trash2, Briefcase } from 'lucide-react';

export default function EmployerInterviewsPage() {
  const { showToast } = useToast();
  const [interviews, setInterviews] = useState([]);
  const [loading, setLoading] = useState(true);

  // Edit Interview Modal
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editTargetInterview, setEditTargetInterview] = useState(null);
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState({
    scheduledAt: '',
    interviewType: 'ONLINE',
    meetingLink: '',
    location: '',
    notes: ''
  });

  useEffect(() => {
    fetchInterviews();
  }, []);

  const fetchInterviews = async () => {
    setLoading(true);
    try {
      const res = await api.get('/interviews/employer');
      const data = res.data.content || res.data || [];
      setInterviews(Array.isArray(data) ? data : []);
    } catch (error) {
      showToast('Failed to load scheduled interviews', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (id, status) => {
    try {
      await api.patch(`/interviews/${id}/status?status=${status}`);
      showToast(`Interview marked as ${status}`, 'success');
      setInterviews(prev => prev.map(i => i.id === id ? { ...i, status } : i));
    } catch (error) {
      showToast('Failed to update interview status', 'error');
    }
  };

  const openEditModal = (interview) => {
    setEditTargetInterview(interview);
    const date = new Date(interview.scheduledAt);
    const localIso = date.toISOString().substring(0, 16);

    setEditForm({
      scheduledAt: localIso,
      interviewType: interview.interviewType || 'ONLINE',
      meetingLink: interview.meetingLink || '',
      location: interview.location || '',
      notes: interview.notes || ''
    });
    setIsEditOpen(true);
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    if (!editTargetInterview) return;

    setEditing(true);
    try {
      const payload = {
        scheduledAt: `${editForm.scheduledAt}:00`,
        locationType: editForm.interviewType,
        locationOrLink: editForm.interviewType === 'ONLINE' ? editForm.meetingLink : editForm.location,
        instructions: editForm.notes
      };

      await api.put(`/interviews/${editTargetInterview.id}`, payload);
      showToast('Interview updated successfully', 'success');
      fetchInterviews();
      setIsEditOpen(false);
    } catch (error) {
      showToast(error.response?.data?.message || 'Failed to update interview', 'error');
    } finally {
      setEditing(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this interview? This action cannot be undone.')) {
      return;
    }

    try {
      await api.delete(`/interviews/${id}`);
      showToast('Interview deleted successfully', 'success');
      setInterviews(prev => prev.filter(i => i.id !== id));
    } catch (error) {
      showToast(error.response?.data?.message || 'Failed to delete interview', 'error');
    }
  };

  if (loading) {
    return <LoadingSpinner text="Loading interview sessions..." />;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Interview Management</h1>
        <p className="text-sm text-gray-500">
          Conduct, track, and complete live evaluation meetings with shortlisted candidates.
        </p>
      </div>

      {interviews.length === 0 ? (
        <div className="card p-12 text-center">
          <Calendar size={40} className="mx-auto text-gray-300 dark:text-gray-600 mb-3" />
          <h3 className="font-bold text-gray-800 dark:text-gray-200">No Interviews Scheduled</h3>
          <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
            To schedule an interview, go to your Job Postings, click Applicants, and click "Schedule Interview" next to any candidate.
          </p>
          <div className="mt-4">
            <Link to="/employer/jobs" className="btn btn-sm btn-primary inline-flex items-center gap-1.5 shadow">
              <Briefcase size={14} /> Go to Job Postings & Applicants
            </Link>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {interviews.map(inv => (
            <div key={inv.id} className="card p-5 border border-gray-200 dark:border-gray-800 hover:shadow-md transition flex flex-col justify-between">
              <div>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h3 className="font-bold text-gray-900 dark:text-white text-base">
                      {inv.candidateName || 'Candidate Interview'}
                    </h3>
                    <p className="text-xs text-primary-600 font-medium">{inv.jobTitle}</p>
                  </div>
                  <StatusBadge status={inv.status} />
                </div>

                <div className="mt-4 p-3 bg-gray-50 dark:bg-gray-800/40 rounded-lg space-y-2 text-xs border border-gray-100 dark:border-gray-800">
                  <div className="flex items-center gap-2">
                    <Calendar size={13} className="text-primary-600" />
                    <span className="font-semibold text-gray-700 dark:text-gray-300">Scheduled:</span>
                    <span>{new Date(inv.scheduledAt).toLocaleString()}</span>
                  </div>

                  <div className="flex items-center gap-2">
                    <Video size={13} className="text-primary-600" />
                    <span className="font-semibold text-gray-700 dark:text-gray-300">Format:</span>
                    <span className="badge badge-info text-[10px]">{inv.interviewType || 'ONLINE'}</span>
                  </div>

                  {inv.notes && (
                    <div className="pt-2 border-t border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400">
                      <span className="font-semibold text-gray-700 dark:text-gray-300">Instructions:</span> {inv.notes}
                    </div>
                  )}
                </div>
              </div>

              <div className="mt-4 pt-3 border-t border-gray-100 dark:border-gray-800 flex items-center justify-between">
                {inv.meetingLink ? (
                  <a
                    href={inv.meetingLink.startsWith('http') ? inv.meetingLink : `https://${inv.meetingLink}`}
                    target="_blank"
                    rel="noreferrer"
                    className="btn btn-xs btn-primary flex items-center gap-1 shadow"
                  >
                    <Video size={12} /> Start Call <ExternalLink size={11} />
                  </a>
                ) : (
                  <span className="text-[11px] text-gray-400">Onsite / Location Meeting</span>
                )}

                <div className="flex gap-2">
                  {inv.status === 'SCHEDULED' && (
                    <>
                      <button
                        onClick={() => openEditModal(inv)}
                        className="btn btn-xs btn-outline text-blue-600 border-blue-300 hover:bg-blue-50"
                        title="Edit Interview"
                      >
                        <Edit2 size={12} />
                      </button>
                      <button
                        onClick={() => handleDelete(inv.id)}
                        className="btn btn-xs btn-outline text-rose-600 border-rose-300 hover:bg-rose-50"
                        title="Delete Interview"
                      >
                        <Trash2 size={12} />
                      </button>
                      <button
                        onClick={() => handleUpdateStatus(inv.id, 'COMPLETED')}
                        className="btn btn-xs btn-outline text-emerald-600 border-emerald-300 hover:bg-emerald-50"
                        title="Mark Completed"
                      >
                        <CheckCircle size={12} /> Done
                      </button>
                      <button
                        onClick={() => handleUpdateStatus(inv.id, 'CANCELLED')}
                        className="btn btn-xs btn-outline text-rose-600 border-rose-300 hover:bg-rose-50"
                        title="Cancel Interview"
                      >
                        <XCircle size={12} /> Cancel
                      </button>
                    </>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Edit Interview Modal */}
      <Modal
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        title={`Edit Interview: ${editTargetInterview?.candidateName || 'Candidate'}`}
      >
        <form onSubmit={handleEditSubmit} className="space-y-4">
          <Input
            label="Date & Time *"
            type="datetime-local"
            value={editForm.scheduledAt}
            onChange={(e) => setEditForm({ ...editForm, scheduledAt: e.target.value })}
            required
          />

          <Select
            label="Interview Format"
            value={editForm.interviewType}
            onChange={(e) => setEditForm({ ...editForm, interviewType: e.target.value })}
            options={[
              { value: 'ONLINE', label: 'Online Video Call' },
              { value: 'IN_PERSON', label: 'In-Person Onsite' },
              { value: 'PHONE', label: 'Phone Screening' }
            ]}
          />

          {editForm.interviewType === 'ONLINE' ? (
            <Input
              label="Meeting Link / Online URL"
              value={editForm.meetingLink}
              onChange={(e) => setEditForm({ ...editForm, meetingLink: e.target.value })}
              placeholder="e.g. Google Meet, Zoom, MS Teams link"
              required
            />
          ) : (
            <Input
              label="Physical Location / Office Address"
              value={editForm.location}
              onChange={(e) => setEditForm({ ...editForm, location: e.target.value })}
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
              value={editForm.notes}
              onChange={(e) => setEditForm({ ...editForm, notes: e.target.value })}
              className="input"
              placeholder="Provide agenda, interviewers, or preparation tips..."
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-gray-200 dark:border-gray-800">
            <button
              type="button"
              onClick={() => setIsEditOpen(false)}
              className="btn btn-outline"
            >
              Cancel
            </button>
            <Button
              type="submit"
              variant="primary"
              loading={editing}
              className="flex items-center gap-2"
            >
              <Edit2 size={16} /> Update Interview
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
