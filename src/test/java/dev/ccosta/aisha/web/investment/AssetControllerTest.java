package dev.ccosta.aisha.web.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.investment.AssetPositionDetails;
import dev.ccosta.aisha.application.investment.AssetPositionService;
import dev.ccosta.aisha.application.investment.AssetService;
import dev.ccosta.aisha.application.investment.AssetCurrentPosition;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetIndexerType;
import dev.ccosta.aisha.domain.investment.AssetType;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;

@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

    @Mock
    private AssetService assetService;

    @Mock
    private AssetPositionService assetPositionService;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AssetController assetController;

    @Test
    void shouldCreateAssetFromForm() {
        AssetForm form = baseForm();

        String view = assetController.create(
            form,
            new BeanPropertyBindingResult(form, "form"),
            "/investments/assets?page=2&size=50",
            new ConcurrentModel()
        );

        assertThat(view).isEqualTo("redirect:/investments/assets?page=2&size=50");
        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetService).create(assetCaptor.capture(), org.mockito.ArgumentMatchers.eq(10L));
        assertThat(assetCaptor.getValue().getType()).isEqualTo(AssetType.STOCK);
        assertThat(assetCaptor.getValue().getName()).isEqualTo("PETR4");
        assertThat(assetCaptor.getValue().getTicker()).isEqualTo("PETR4");
        assertThat(assetCaptor.getValue().getCurrency()).isEqualTo("BRL");
        assertThat(assetCaptor.getValue().getOpeningPositionDate()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(assetCaptor.getValue().getOpeningPositionQuantity()).isEqualByComparingTo("12.5000000000");
        assertThat(assetCaptor.getValue().getOpeningPositionTotalCost()).isEqualByComparingTo("1234.56");
        assertThat(assetCaptor.getValue().getOpeningPositionCurrency()).isEqualTo("BRL");
    }

    @Test
    void shouldFallbackToAssetListingWhenReturnPathIsUnsafe() {
        AssetForm form = baseForm();

        String view = assetController.create(
            form,
            new BeanPropertyBindingResult(form, "form"),
            "https://evil.example/investments/assets",
            new ConcurrentModel()
        );

        assertThat(view).isEqualTo("redirect:/investments/assets");
    }

    @Test
    void shouldRepopulateFormModelWhenValidationFails() {
        AssetForm form = baseForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("name", "assetForm.name.notBlank");
        Account account = new Account();
        account.setTitle("Corretora");

        when(accountService.listAvailableForEntryForm(10L)).thenReturn(List.of(account));

        ConcurrentModel model = new ConcurrentModel();
        String view = assetController.create(form, bindingResult, "/investments/assets", model);

        assertThat(view).isEqualTo("investments/assets/form");
        assertThat(model.getAttribute("assetTypes")).isEqualTo(AssetType.values());
        assertThat(model.getAttribute("indexerTypes")).isEqualTo(AssetIndexerType.values());
        assertThat(model.getAttribute("accounts")).isEqualTo(List.of(account));
    }

    @Test
    void shouldBulkDeleteAndRefreshListingForHtmx() {
        when(assetService.listPageOrdered(null, null, null, 0, 25)).thenReturn(new PagedResult<>(List.of(), 0, 25, 0, 0));

        String view = assetController.bulkDelete(List.of(1L, 2L), null, null, null, null, null, htmxRequest(), new ConcurrentModel());

        assertThat(view).isEqualTo("investments/assets/list :: table");
        verify(assetService).bulkDelete(List.of(1L, 2L));
        verify(assetService).listPageOrdered(null, null, null, 0, 25);
    }

    @Test
    void shouldApplyListingFilters() {
        when(assetService.listPageOrdered(10L, AssetType.STOCK, "Petróleo", 0, 25))
            .thenReturn(new PagedResult<>(List.of(), 0, 25, 0, 0));

        ConcurrentModel model = new ConcurrentModel();
        String view = assetController.table(10L, AssetType.STOCK, " Petróleo ", null, null, model);

        assertThat(view).isEqualTo("investments/assets/list :: table");
        assertThat(model.getAttribute("selectedAccountId")).isEqualTo(10L);
        assertThat(model.getAttribute("selectedAssetType")).isEqualTo(AssetType.STOCK);
        assertThat(model.getAttribute("selectedDescription")).isEqualTo("Petróleo");
        verify(assetService).listPageOrdered(10L, AssetType.STOCK, "Petróleo", 0, 25);
    }

    @Test
    void shouldOpenAssetDetailsPage() {
        Asset asset = new Asset();
        asset.setName("PETR4");
        AssetPositionDetails details = new AssetPositionDetails(
            asset,
            new AssetCurrentPosition(
                new BigDecimal("12.0000000000"),
                new BigDecimal("1234.56"),
                new BigDecimal("102.88000000"),
                "BRL",
                LocalDate.of(2026, 2, 20)
            ),
            List.of(),
            List.of()
        );

        when(assetPositionService.buildDetails(15L)).thenReturn(details);

        ConcurrentModel model = new ConcurrentModel();
        String view = assetController.details(15L, "/investments/assets?page=1", model);

        assertThat(view).isEqualTo("investments/assets/details");
        assertThat(model.getAttribute("details")).isEqualTo(details);
        assertThat(model.getAttribute("returnTo")).isEqualTo("/investments/assets?page=1");
    }

    private AssetForm baseForm() {
        AssetForm form = new AssetForm();
        form.setAccountId(10L);
        form.setType(AssetType.STOCK);
        form.setName("PETR4");
        form.setTicker("PETR4");
        form.setCurrency("BRL");
        form.setIndexerType(AssetIndexerType.NONE);
        form.setMaturityDate(LocalDate.of(2029, 1, 1));
        form.setOpeningPositionDate(LocalDate.of(2026, 1, 31));
        form.setOpeningPositionQuantity(new BigDecimal("12.5000000000"));
        form.setOpeningPositionTotalCost(new BigDecimal("1234.56"));
        form.setOpeningPositionCurrency("BRL");
        return form;
    }

    private MockHttpServletRequest htmxRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("HX-Request", "true");
        return request;
    }
}
