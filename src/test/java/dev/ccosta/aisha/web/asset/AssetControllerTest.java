package dev.ccosta.aisha.web.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.asset.AssetInUseException;
import dev.ccosta.aisha.application.asset.AssetPositionDetails;
import dev.ccosta.aisha.application.asset.AssetPositionService;
import dev.ccosta.aisha.application.asset.AssetService;
import dev.ccosta.aisha.application.asset.AssetCurrentPosition;
import dev.ccosta.aisha.domain.asset.Asset;
import dev.ccosta.aisha.domain.asset.AssetIndexerType;
import dev.ccosta.aisha.domain.asset.AssetType;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;

@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

    @Mock
    private AssetService assetService;

    @Mock
    private AssetPositionService assetPositionService;

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
        verify(assetService).create(assetCaptor.capture());
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
    void shouldCreateAssetWithoutOpeningPositionWhenOpeningFieldsAreBlank() {
        AssetForm form = baseForm();
        form.setOpeningPositionDate(null);
        form.setOpeningPositionQuantity(null);
        form.setOpeningPositionTotalCost(null);
        form.setOpeningPositionCurrency("");

        String view = assetController.create(
            form,
            new BeanPropertyBindingResult(form, "form"),
            "/investments/assets",
            new ConcurrentModel()
        );

        assertThat(view).isEqualTo("redirect:/investments/assets");
        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetService).create(assetCaptor.capture());
        assertThat(assetCaptor.getValue().getOpeningPosition()).isNull();
    }

    @Test
    void shouldReturnFormErrorWhenOpeningPositionIsPartial() {
        AssetForm form = baseForm();
        form.setOpeningPositionDate(null);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        ConcurrentModel model = new ConcurrentModel();
        String view = assetController.create(form, bindingResult, "/investments/assets", model);

        assertThat(view).isEqualTo("investments/assets/form");
        assertThat(bindingResult.hasFieldErrors("openingPositionDate")).isTrue();
        verify(assetService, never()).create(any());
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
        ConcurrentModel model = new ConcurrentModel();
        String view = assetController.create(form, bindingResult, "/investments/assets", model);

        assertThat(view).isEqualTo("investments/assets/form");
        assertThat(model.getAttribute("assetTypes")).isEqualTo(AssetType.values());
        assertThat(model.getAttribute("indexerTypes")).isEqualTo(AssetIndexerType.values());
    }

    @Test
    void shouldBulkDeleteAndRefreshListingForHtmx() {
        when(assetService.listPageOrdered(null, null, 0, 25)).thenReturn(new PagedResult<>(List.of(), 0, 25, 0, 0));

        String view = assetController.bulkDelete(List.of(1L, 2L), null, null, null, null, htmxRequest(), new ConcurrentModel());

        assertThat(view).isEqualTo("investments/assets/list :: table");
        verify(assetService).bulkDelete(List.of(1L, 2L));
        verify(assetService).listPageOrdered(null, null, 0, 25);
    }

    @Test
    void shouldReturnListingErrorWhenDeletingAssetInUseForHtmx() {
        doThrow(new AssetInUseException(7L)).when(assetService).deleteById(7L);
        when(assetService.listPageOrdered(AssetType.STOCK, "PETR4", 1, 50))
            .thenReturn(new PagedResult<>(List.of(), 1, 50, 51, 2));

        ConcurrentModel model = new ConcurrentModel();
        String view = assetController.delete(7L, AssetType.STOCK, " PETR4 ", 1, 50, htmxRequest(), model);

        assertThat(view).isEqualTo("investments/assets/list :: table");
        assertThat(model.getAttribute("hasError")).isEqualTo(true);
        assertThat(model.getAttribute("selectedAssetType")).isEqualTo(AssetType.STOCK);
        assertThat(model.getAttribute("selectedDescription")).isEqualTo("PETR4");
        verify(assetService).listPageOrdered(AssetType.STOCK, "PETR4", 1, 50);
    }

    @Test
    void shouldReturnListingErrorWhenBulkDeletingAssetInUseForHtmx() {
        List<Long> ids = List.of(7L, 8L);
        doThrow(new AssetInUseException(7L)).when(assetService).bulkDelete(ids);
        when(assetService.listPageOrdered(null, null, 0, 25)).thenReturn(new PagedResult<>(List.of(), 0, 25, 0, 0));

        ConcurrentModel model = new ConcurrentModel();
        String view = assetController.bulkDelete(ids, null, null, null, null, htmxRequest(), model);

        assertThat(view).isEqualTo("investments/assets/list :: table");
        assertThat(model.getAttribute("hasError")).isEqualTo(true);
        verify(assetService).listPageOrdered(null, null, 0, 25);
    }

    @Test
    void shouldApplyListingFilters() {
        when(assetService.listPageOrdered(AssetType.STOCK, "Petróleo", 0, 25))
            .thenReturn(new PagedResult<>(List.of(), 0, 25, 0, 0));

        ConcurrentModel model = new ConcurrentModel();
        MockHttpServletRequest request = htmxRequest();
        request.setQueryString("type=STOCK&description=Petr%C3%B3leo&page=0&size=25");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String view = assetController.table(AssetType.STOCK, " Petróleo ", null, null, request, response, model);

        assertThat(view).isEqualTo("investments/assets/list :: table");
        assertThat(response.getHeader("HX-Push-Url"))
            .isEqualTo("/investments/assets?type=STOCK&description=Petr%C3%B3leo&page=0&size=25");
        assertThat(model.getAttribute("selectedAssetType")).isEqualTo(AssetType.STOCK);
        assertThat(model.getAttribute("selectedDescription")).isEqualTo("Petróleo");
        verify(assetService).listPageOrdered(AssetType.STOCK, "Petróleo", 0, 25);
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
