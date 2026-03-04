package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.model.FAQ;
import com.vbs.capsAllocation.repository.FAQRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class FAQService {

    @Autowired
    private FAQRepository faqRepository;

    // Users who can manage FAQs
    private static final List<String> FAQ_MANAGERS = Arrays.asList("piyushmi", "vrajoriya");

    public boolean canManageFAQs(String username) {
        return FAQ_MANAGERS.contains(username.toLowerCase());
    }

    public List<FAQ> getActiveFAQs() {
        return faqRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    public List<FAQ> getAllFAQs() {
        return faqRepository.findAllByOrderByDisplayOrderAsc();
    }

    public FAQ getFAQById(Long id) {
        return faqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ not found with id: " + id));
    }

    @Transactional
    public FAQ createFAQ(FAQ faq, String username) {
        if (!canManageFAQs(username)) {
            throw new SecurityException("You are not authorized to manage FAQs");
        }
        faq.setCreatedBy(username);
        return faqRepository.save(faq);
    }

    @Transactional
    public FAQ updateFAQ(Long id, FAQ updatedFaq, String username) {
        if (!canManageFAQs(username)) {
            throw new SecurityException("You are not authorized to manage FAQs");
        }

        FAQ existing = getFAQById(id);
        existing.setQuestion(updatedFaq.getQuestion());
        existing.setAnswer(updatedFaq.getAnswer());
        existing.setCategory(updatedFaq.getCategory());
        existing.setDisplayOrder(updatedFaq.getDisplayOrder());
        existing.setIsActive(updatedFaq.getIsActive());
        existing.setUpdatedBy(username);

        return faqRepository.save(existing);
    }

    @Transactional
    public void deleteFAQ(Long id, String username) {
        if (!canManageFAQs(username)) {
            throw new SecurityException("You are not authorized to manage FAQs");
        }
        faqRepository.deleteById(id);
    }

    /**
     * Initialize default FAQs if none exist
     */
    @Transactional
    public void initializeDefaultFAQs(String username) {
        if (faqRepository.count() == 0) {
            List<FAQ> defaultFAQs = Arrays.asList(
                    new FAQ(
                            "I'm having trouble logging in. What should I do?",
                            "Try clearing your browser cache and cookies before attempting to log in again. If the issue persists, ensure you're using the correct credentials and try using an incognito/private browser window.",
                            "Troubleshooting",
                            1),
                    new FAQ(
                            "My session keeps timing out. How can I fix this?",
                            "Session timeouts are normal after periods of inactivity. Make sure to save your work frequently. If you experience frequent unexpected timeouts, try logging out, clearing your cache, and logging back in.",
                            "Troubleshooting",
                            2),
                    new FAQ(
                            "The page is not loading correctly. What should I do?",
                            "Try these steps: 1) Refresh the page (Ctrl+F5 for hard refresh), 2) Clear browser cache and cookies, 3) Try a different browser, 4) Check your internet connection. If the issue persists, contact the development team.",
                            "Troubleshooting",
                            3),
                    new FAQ(
                            "How do I submit a time entry?",
                            "Navigate to 'Time Entry' from the sidebar menu. Select the date, project, and activity type. Enter the hours worked and add any notes. Click 'Submit' to save your time entry.",
                            "General",
                            4),
                    new FAQ(
                            "How do I apply for leave?",
                            "Go to 'Leaves & WFH' from the sidebar. Click 'Apply Leave/WFH', select the type (Casual Leave, Sick Leave, WFH, etc.), choose the dates, and submit the request for approval.",
                            "General",
                            5),
                    new FAQ(
                            "Where can I view my attendance records?",
                            "Navigate to 'Attendance' from the sidebar menu. You can view your check-in/check-out times, work hours, and attendance history for any date range.",
                            "General",
                            6),
                    new FAQ(
                            "How do I view release notes and updates?",
                            "When a new release is published, you'll receive an email notification. You can also access release notes from the Release Management section (Admin access required).",
                            "General",
                            7),
                    new FAQ(
                            "Who do I contact for technical support?",
                            "For technical issues, please reach out to the development team via email or through your team lead. For urgent issues, contact piyushmi or vrajoriya directly.",
                            "Support",
                            8));

            for (FAQ faq : defaultFAQs) {
                faq.setCreatedBy(username);
                faqRepository.save(faq);
            }
        }
    }
}
