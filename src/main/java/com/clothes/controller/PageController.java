package com.clothes.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for static pages
 */
@Controller
public class PageController {

    /**
     * Contact page
     */
    @GetMapping("/contact")
    public String showContactPage() {
        return "contact";
    }

    /**
     * Privacy policy page
     */
    @GetMapping("/policy/privacy")
    public String showPrivacyPolicyPage() {
        return "policy-privacy";
    }

    /**
     * Terms of service page
     */
    @GetMapping("/policy/terms")
    public String showTermsPage() {
        return "policy-terms";
    }

    /**
     * Shipping policy page
     */
    @GetMapping("/policy/shipping")
    public String showShippingPolicyPage() {
        return "policy-shipping";
    }

    /**
     * FAQ page
     */
    @GetMapping("/faq")
    public String showFaqPage() {
        return "faq";
    }
}
